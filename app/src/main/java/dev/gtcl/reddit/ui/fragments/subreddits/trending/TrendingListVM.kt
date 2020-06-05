package dev.gtcl.reddit.ui.fragments.subreddits.trending

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class TrendingListVM(application: RedditApplication): AndroidViewModel(application){

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _items = MutableLiveData<ArrayList<TrendingSubredditPost>>()
    val items: LiveData<ArrayList<TrendingSubredditPost>>
        get() = _items

    private val _newItems = MutableLiveData<List<TrendingSubredditPost>>()
    val newItems: LiveData<List<TrendingSubredditPost>>
        get() = _newItems

    private val loadedIds = HashSet<String>()

    private var after: String? = null

    private var favoriteSubsHash: HashSet<String>? = null
    private var subscribedSubsHash: HashSet<String>? = null

    private val _subscribedSubs = MutableLiveData<HashSet<String>?>()
    val subscribedSubs: LiveData<HashSet<String>?>
        get() = _subscribedSubs

    private val _refreshState = MutableLiveData<NetworkState>()
    val refreshState: LiveData<NetworkState>
        get() = _refreshState

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    var initialPageLoaded = false
    private var lastItemReached = false

    fun syncWithDb(){
        coroutineScope.launch {
            withContext(Dispatchers.Default){
                subscribedSubsHash = subredditRepository.getMySubscriptions(SubscriptionType.SUBREDDIT).map { it.displayName.toLowerCase(Locale.ENGLISH) }.toHashSet()
                _subscribedSubs.postValue(subscribedSubsHash!!)
            }
        }
    }

    fun subredditsSynced(){
        _subscribedSubs.value = null
    }

    fun retry(){
        TODO()
    }

    fun loadInitialDataAndFirstPage(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            loadFirstPage()
            _networkState.value = NetworkState.LOADED
        }
    }

    fun refresh(){
        coroutineScope.launch {
            _refreshState.value = NetworkState.LOADING
            loadFirstPage()
            _refreshState.value = NetworkState.LOADED
        }
    }

    private suspend fun loadFirstPage(){
        withContext(Dispatchers.Default){
            try {
                val response = listingRepository.getListing(TRENDING_LISTING, SORT, null, null, PAGE_SIZE * 3).await()
                val items = ArrayList(response.data.children.map { TrendingSubredditPost(it.data as Post) })

                if(subscribedSubsHash == null){
                    subscribedSubsHash = subredditRepository.getMySubscriptions(SubscriptionType.SUBREDDIT).map { it.displayName.toLowerCase(Locale.ENGLISH) }.toHashSet()
                }
                setSubsAndFavoritesInTrendingPost(items, subscribedSubsHash!!)
                _items.postValue(items)
                lastItemReached = items.size < (PAGE_SIZE)
                loadedIds.clear()
                loadedIds.addAll(items.map { it.post.id })
                after = response.data.after
            } catch (e: Exception){
                _errorMessage.postValue(e.toString())
            }
        }
    }

    fun loadAfter() {
        if(lastItemReached){
            return
        }
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            withContext(Dispatchers.Default) {
                try{
                    val response = listingRepository.getListing(TRENDING_LISTING, SORT, null, after, PAGE_SIZE).await()
                    val newItems = response.data.children.map { TrendingSubredditPost(it.data as Post) }.filter { !loadedIds.contains(it.post.id) }

                    lastItemReached = newItems.isEmpty()
                    if(lastItemReached){
                        _networkState.value = NetworkState.LOADED
                        return@withContext
                    }

                    if(subscribedSubsHash == null){
                        subscribedSubsHash = subredditRepository.getMySubscriptions(SubscriptionType.SUBREDDIT).map { it.displayName.toLowerCase(Locale.ENGLISH) }.toHashSet()
                    }
                    setSubsAndFavoritesInTrendingPost(newItems, subscribedSubsHash!!)

                    loadedIds.addAll(newItems.map { it.post.id })
                    _items.value!!.addAll(newItems)
                    _newItems.postValue(newItems.toList())
                    after = response.data.after
                } catch (e: Exception){
                    _errorMessage.postValue(e.toString())
                }
            }
            _networkState.value = NetworkState.LOADED
        }
    }

    fun newItemsAdded(){
        _newItems.value = null
    }

    companion object{
        private val TRENDING_SUBREDDIT = Subreddit("", "trendingsubreddits", "", "", "", false, "", "")
        private val TRENDING_LISTING = SubredditListing(TRENDING_SUBREDDIT)
        private val SORT = PostSort.NEW
        private const val PAGE_SIZE = 7
    }

}