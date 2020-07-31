package dev.gtcl.reddit.ui.fragments.listing

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*

class ListingVM(val application: RedditApplication): AndroidViewModel(application) {

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private val _subreddit = MutableLiveData<Subreddit?>()
    val subreddit: LiveData<Subreddit?>
        get() = _subreddit

    private lateinit var retryLambda: () -> Unit

    fun retry() {
        retryLambda()
    }

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _refreshState = MutableLiveData<NetworkState>()
    val refreshState: LiveData<NetworkState>
        get() = _refreshState

    private val _items = MutableLiveData<MutableList<Item>>()
    val items: LiveData<MutableList<Item>>
        get() = _items

    private val _moreItems = MutableLiveData<List<Item>?>()
    val moreItems: LiveData<List<Item>?>
        get() = _moreItems

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

    private var _lastItemReached = MutableLiveData<Boolean>().apply { value = false }
    val lastItemReached: LiveData<Boolean>
        get() = _lastItemReached

    private var _newMultiReddit = MutableLiveData<MultiReddit>()
    val newMultiReddit: LiveData<MultiReddit>
        get() = _newMultiReddit

    fun setListingInfo(listingType: ListingType){
        this.listingType = listingType
        _title.value = getTitle(listingType)
        fetchSubredditInfo(listingType)
    }

    private fun fetchSubredditInfo(listingType: ListingType){
        coroutineScope.launch {
            val sub = when(listingType){
                is SubredditListing -> subredditRepository.getSubreddit(listingType.sub.displayName).await().data
                is SubscriptionListing -> {
                    val subscription = listingType.subscription
                    if(subscription.type == SubscriptionType.SUBREDDIT){
                        subredditRepository.getSubreddit(subscription.displayName).await().data
                    } else {
                        null
                    }
                }
                else -> null
            }
            _subreddit.value = sub
            if(sub != null){
                syncSubredditWithDatabase()
            }
        }
    }

    fun setSort(postSort: PostSort, time: Time? = null){
        _postSort.value = postSort
        _time.value = time
    }

    fun loadFirstItems(){
        if(lastItemReached.value == true){
            return
        }
        coroutineScope.launch {
            _networkState.postValue(NetworkState.LOADING)
            try {
                // Get listing items
                val size = pageSize * 3
                val response = listingRepository.getListing(listingType, postSort.value!!, time.value, after, size).await()
                val items = response.data.children.map { it.data }.toMutableList()
                listingRepository.getReadPosts().map { it.name }.toCollection(readItemIds)
                setItemsReadStatus(items, readItemIds)
                _items.value = items
                _lastItemReached.value = items.size < size
                after = response.data.after
            } catch (e: Exception){
                retryLambda = ::loadFirstItems
                after = null
            }
            _networkState.postValue(NetworkState.LOADED)
            _refreshState.postValue(NetworkState.LOADED)
            _initialPageLoaded = true
        }
    }

    fun loadMore(){
        if(lastItemReached.value == true){
            return
        }
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            val previousAfter = after
            try {
                // Get listing items
                val size = pageSize
                val response = listingRepository.getListing(listingType, postSort.value!!, time.value, after, size).await()
                val items = response.data.children.map { it.data }.toMutableList()
                listingRepository.getReadPosts().map { it.name }.toCollection(readItemIds)
                setItemsReadStatus(items, readItemIds)
                _moreItems.value = items
                _items.value?.addAll(items)
                _lastItemReached.value = items.size < size
                after = response.data.after
                _networkState.value = NetworkState.LOADED
            } catch (e: Exception){
                after = previousAfter
                retryLambda = ::loadMore
                _networkState.value = NetworkState.error(e.getErrorMessage(application))
            }
        }
    }

    fun moreItemsObserved(){
        _moreItems.value = null
    }

    fun refresh(){
        coroutineScope.launch {
            _refreshState.value = NetworkState.LOADING
            after = null
            _items.value?.clear()
            _lastItemReached.value = false
            loadFirstItems()
        }
    }

    fun removeItemAt(position: Int){
        _items.value?.removeAt(position)
    }

    fun addReadItem(item: Item){
        readItemIds.add(item.name)
        coroutineScope.launch {
            listingRepository.addReadItem(item)
        }
    }

    fun updateItem(item: Item, position: Int){
        _items.updateItem(item, position)
    }

    // Right Side Bar Layout
    fun syncSubreddit(){
        coroutineScope.launch {
            syncSubredditWithDatabase()
        }
    }

    private suspend fun syncSubredditWithDatabase(){
        withContext(Dispatchers.IO){
            val subreddit = subreddit.value ?: return@withContext
            val subscription = subredditRepository.getMySubscription(subreddit.name)
            subreddit.userSubscribed = subscription != null
            subreddit.setFavorite(subscription?.isFavorite ?: false)
            _subreddit.postValue(subreddit)
        }
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