package dev.gtcl.reddit.ui.fragments.subreddits.trending

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.DbMultiReddit
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

    private val _favoriteSubs = MutableLiveData<HashSet<String>?>()
    val favoriteSubs: LiveData<HashSet<String>?>
        get() = _favoriteSubs

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
                favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName.toLowerCase(Locale.ENGLISH) }.toHashSet()
                _favoriteSubs.postValue(favoriteSubsHash!!)
                subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName.toLowerCase(Locale.ENGLISH) }.toHashSet()
                _subscribedSubs.postValue(subscribedSubsHash!!)
            }
        }
    }

    fun favoriteSubsSynced(){
        _favoriteSubs.value = null
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

                if(favoriteSubsHash == null){
                    favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName.toLowerCase(Locale.ENGLISH) }.toHashSet()
                }
                if(subscribedSubsHash == null){
                    subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName.toLowerCase(Locale.ENGLISH) }.toHashSet()
                }
                setSubsAndFavoritesInTrendingPost(items, subscribedSubsHash!!, favoriteSubsHash!!)
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

                    if(favoriteSubsHash == null){
                        favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName.toLowerCase(Locale.ENGLISH) }.toHashSet()
                    }
                    if(subscribedSubsHash == null){
                        subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName.toLowerCase(Locale.ENGLISH) }.toHashSet()
                    }
                    setSubsAndFavoritesInTrendingPost(newItems, subscribedSubsHash!!, favoriteSubsHash!!)

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

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    fun subscribe(subreddit: Subreddit, subscribeAction: SubscribeAction, favorite: Boolean){
        coroutineScope.launch {
            subredditRepository.subscribe(subreddit.displayName, subscribeAction).enqueue(object:
                Callback<Void> {
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    _errorMessage.value = t.message
                }

                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    coroutineScope.launch {
                        if(subscribeAction == SubscribeAction.SUBSCRIBE) insertSub(subreddit, favorite)
                        else subredditRepository.removeSubreddit(subreddit)
                    }
                }
            })
        }
    }

    fun favorite(subreddit: Subreddit, favorite: Boolean){
        coroutineScope.launch {
            if(favorite) {
                subscribe(subreddit, SubscribeAction.SUBSCRIBE, favorite)
            } else {
                subredditRepository.addToFavorites(subreddit.displayName, favorite)
            }
        }
    }

    suspend fun insertSub(subreddit: Subreddit, favorite: Boolean){
        val sub: Subreddit = if(subreddit.name == ""){
            (subredditRepository
                .searchSubreddits(nsfw = true, includeProfiles = false, limit = 1, query = subreddit.displayName)
                .await()
                .data
                .children[0] as SubredditChild)
                .data
        } else {
            subreddit
        }
        sub.isFavorite = favorite
        subredditRepository.insertSubreddit(sub)
    }

    companion object{
        private val TRENDING_SUBREDDIT = Subreddit("", "trendingsubreddits", "", "", "", false, "")
        private val TRENDING_LISTING = SubredditListing(TRENDING_SUBREDDIT)
        private val SORT = PostSort.NEW
        private const val PAGE_SIZE = 7
    }

}