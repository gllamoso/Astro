package dev.gtcl.reddit.ui.fragments.item_scroller

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.ListingType
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.MessageRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class ItemScrollerVM(application: RedditApplication): AndroidViewModel(application){

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)
    private val messageRepository = MessageRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _items = MutableLiveData<MutableList<Item>>()
    val items: LiveData<MutableList<Item>>
        get() = _items

    private val _moreItems = MutableLiveData<List<Item>?>()
    val moreItems: LiveData<List<Item>?>
        get() = _moreItems

    private val readItemIds = HashSet<String>()
    private var after: String? = null
    var user: String? = null

    private lateinit var postSort: PostSort
    private var t: Time? = null
    private var pageSize = 15
    private var _initialPageLoaded = false
    val initialPageLoaded: Boolean
        get() = _initialPageLoaded

    private val _lastItemReached = MutableLiveData<Boolean>().apply { value = false }
    val lastItemReached: LiveData<Boolean>
        get() = _lastItemReached

    private val _refreshState = MutableLiveData<NetworkState>()
    val refreshState: LiveData<NetworkState>
        get() = _refreshState

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    fun retry(){
        TODO()
    }

    private lateinit var listingType: ListingType
    fun setListingInfo(listingType: ListingType, postSort: PostSort, t: Time?, pageSize: Int){
        this.listingType = listingType
        this.postSort = postSort
        this.t = t
        this.pageSize = pageSize
    }

    private lateinit var subredditWhere: SubredditWhere
    fun setListingInfo(subredditWhere: SubredditWhere, pageSize: Int){
        this.subredditWhere = subredditWhere
        this.pageSize = pageSize
    }

    private lateinit var messageWhere: MessageWhere
    fun setListingInfo(messageWhere: MessageWhere, pageSize: Int){
        this.messageWhere = messageWhere
        this.pageSize = pageSize
    }

    fun loadFirstItems(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            loadFirsItemsSuspend()
            _networkState.value = NetworkState.LOADED
            _initialPageLoaded = true
        }
    }

    fun loadMore(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            loadMoreSuspend()
            _networkState.value = NetworkState.LOADED
        }
    }

    fun moreItemsObserved(){
        _moreItems.value = null
    }

    fun addReadItem(item: Item){
        readItemIds.add(item.name)
        coroutineScope.launch {
            listingRepository.addReadItem(item)
        }
    }

    fun refresh(){
        coroutineScope.launch {
            _refreshState.value = NetworkState.LOADING
            after = null
            _items.value?.clear()
            loadMore()
            _refreshState.value = NetworkState.LOADED
        }
    }

    private suspend fun loadFirsItemsSuspend(){
        try {
            val size = pageSize * 3
            val response = when{
                ::listingType.isInitialized -> listingRepository.getListing(listingType, postSort, t, after, size, user).await()
                ::subredditWhere.isInitialized -> subredditRepository.getSubredditsListing(subredditWhere, after, size).await()
                ::messageWhere.isInitialized -> messageRepository.getMessages(messageWhere, after, size).await()
                else -> throw IllegalStateException("Not enough info to load listing")
            }
            val items = response.data.children.map { it.data }.toMutableList()
            listingRepository.getReadPosts().map { it.name }.toCollection(readItemIds)
            setItemsReadStatus(items, readItemIds)
            _items.value = items
            _lastItemReached.value = items.size < size
            after = response.data.after
        } catch (e: Exception){
            _errorMessage.value = e.toString()
        }
    }

    private suspend fun loadMoreSuspend(){
        try {
            val size = pageSize
            val response = when{
                ::listingType.isInitialized -> listingRepository.getListing(listingType, postSort, t, after, size, user).await()
                ::subredditWhere.isInitialized -> subredditRepository.getSubredditsListing(subredditWhere, after, size).await()
                ::messageWhere.isInitialized -> messageRepository.getMessages(messageWhere, after, size).await()
                else -> throw IllegalStateException("Not enough info to load listing")
            }
            val items = response.data.children.map { it.data }.toMutableList()
            _moreItems.value = items
            _items.value?.addAll(items)
            _lastItemReached.value = items.size < size
            after = response.data.after
        } catch (e: Exception){
            _errorMessage.value = e.toString()
        }
    }

    fun removeItemAt(position: Int){
        _items.value?.removeAt(position)
    }

}