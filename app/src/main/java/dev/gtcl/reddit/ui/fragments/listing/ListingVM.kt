package dev.gtcl.reddit.ui.fragments.listing

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.*

class ListingVM(val application: RedditApplication): AndroidViewModel(application) {

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private val _subreddit = MutableLiveData<Subreddit>()
    val subreddit: LiveData<Subreddit>
        get() = _subreddit

//
    fun retry() {
//        val listing = postListingsOfSubreddit.value
//        listing?.retry?.invoke()
    }

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _refreshState = MutableLiveData<NetworkState>()
    val refreshState: LiveData<NetworkState>
        get() = _refreshState

    private val _items = MutableLiveData<ArrayList<Item>>()
    val items: LiveData<ArrayList<Item>>
        get() = _items

    private val _newItems = MutableLiveData<List<Item>>()
    val newItems: LiveData<List<Item>>
        get() = _newItems

    private val loadedIds = HashSet<String>()

    private val readItemIds = HashSet<String>()
    private var after: String? = null

    private lateinit var listingType: ListingType

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _postSort = MutableLiveData<PostSort>().apply { value = PostSort.HOT }
    val postSort: LiveData<PostSort>
        get() = _postSort

    private val _time = MutableLiveData<Time?>().apply { value = null }
    val time: LiveData<Time?>
        get() = _time

    private var pageSize = 15

    private var _initialPageLoaded = false
    val initialPageLoaded: Boolean
        get() = _initialPageLoaded

    private var _lastItemReached = MutableLiveData<Boolean>()
    val lastItemReached: LiveData<Boolean>
        get() = _lastItemReached

    fun setListingInfo(listingType: ListingType){
        this.listingType = listingType
    }

    fun setSort(postSort: PostSort, time: Time? = null){
        _postSort.value = postSort
        _time.value = time
    }

    fun loadFirstPage(){
        coroutineScope.launch {
            _networkState.postValue(NetworkState.LOADING)
            loadFirstPageAndData()
            _networkState.postValue(NetworkState.LOADED)
            _initialPageLoaded = true
        }
    }

    private suspend fun loadFirstPageAndData(){
        try {
            _title.value = getTitle(listingType)

            // Load subreddit Info
            val sub = when(val listing = listingType){
                is SubredditListing ->{ subredditRepository.getSubredditInfo(listing.sub.displayName).await().data }
                is SubscriptionListing -> {
                    val subscription = listing.subscription
                    if(subscription.type == SubscriptionType.SUBREDDIT){
                        subredditRepository.getSubredditInfo(subscription.displayName).await().data
                    } else {
                        null
                    }
                }
                else -> null
            }

            _subreddit.value = sub
            syncSubredditWithDatabase()

            // Get listing items
            val response = listingRepository.getListing(listingType, postSort.value!!, time.value, null, pageSize * 3).await()
            val items = ArrayList(response.data.children.map { it.data })
            listingRepository.getReadPosts().map { it.name }.toCollection(readItemIds)
            setItemsReadStatus(items, readItemIds)
            _items.postValue(items)
            _lastItemReached.value = items.size < (pageSize * 3)
            loadedIds.clear()
            loadedIds.addAll(items.map { it.name })
            after = response.data.after
        } catch (e: Exception){
            _errorMessage.value = e.toString()
        }
    }

    fun loadAfter(){
        if(lastItemReached.value == true){
            return
        }
        coroutineScope.launch {
            withContext(Dispatchers.Default){
                _networkState.postValue(NetworkState.LOADING)
                try{
                    val response = listingRepository.getListing(
                        listingType,
                        postSort.value!!,
                        time.value,
                        after,
                        pageSize
                    ).await()
                    val newItems = response.data.children.map { it.data }.filter { !loadedIds.contains(it.name) }

                    _lastItemReached.postValue(newItems.isEmpty())
                    if(lastItemReached.value == true){
                        _networkState.postValue(NetworkState.LOADED)
                        return@withContext
                    }

                    loadedIds.addAll(newItems.map { it.name })
                    setItemsReadStatus(newItems, readItemIds)
                    _items.value!!.addAll(newItems)
                    _newItems.postValue(newItems.toList())
                    after = response.data.after
                    _networkState.postValue(NetworkState.LOADED)
                } catch (e: Exception){
                    _errorMessage.postValue(e.toString())
                }
            }
        }
    }

    fun newItemsAdded(){
        _newItems.value = null
    }

    fun refresh(){
        coroutineScope.launch {
            _refreshState.value = NetworkState.LOADING
            loadFirstPageAndData()
            _refreshState.value = NetworkState.LOADED
        }
    }

    // Right Side Bar Layout
    fun syncSubreddit(){
        coroutineScope.launch {
            syncSubredditWithDatabase()
        }
    }

    private suspend fun syncSubredditWithDatabase(){
        val subreddit = subreddit.value ?: return
        val subscription = subredditRepository.getMySubscription(subreddit.name)
        subreddit.userSubscribed = subscription != null
        subreddit.isFavorite = subscription?.isFavorite ?: false
        _subreddit.value = null
        _subreddit.value = subreddit
    }

    private fun getTitle(listingType: ListingType): String{
        return when(listingType){
            is FrontPage -> application.getString(R.string.frontpage)
            is All -> application.getString(R.string.all)
            is Popular -> application.getString(R.string.popular_tab_label)
            is MultiRedditListing -> listingType.multiReddit.displayName
            is SubredditListing -> listingType.sub.displayName
            is SubscriptionListing -> listingType.subscription.displayName
            is ProfileListing -> listingType.info.name
        }
    }

}