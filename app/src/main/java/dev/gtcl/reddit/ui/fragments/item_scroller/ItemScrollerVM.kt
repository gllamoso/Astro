package dev.gtcl.reddit.ui.fragments.item_scroller

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.MessageRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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

    private val _items = MutableLiveData<ArrayList<Item>>()
    val items: LiveData<ArrayList<Item>>
        get() = _items

    private val _newItems = MutableLiveData<List<Item>>()
    val newItems: LiveData<List<Item>>
        get() = _newItems

    private val loadedIds = HashSet<String>()

    private val readItemIds = HashSet<String>()
    private var after: String? = null
    var user: String? = null

    private lateinit var postSort: PostSort
    private var t: Time? = null
    private var pageSize = 15
    var initialPageLoaded = false
    private var lastItemReached = false

    private var favoriteSubsHash: java.util.HashSet<String>? = null
    private var subscribedSubsHash: java.util.HashSet<String>? = null

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

    fun syncWithDb(){
        coroutineScope.launch {
            withContext(Dispatchers.Default){
                favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName }.toHashSet()
                _favoriteSubs.postValue(favoriteSubsHash!!)
                subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName }.toHashSet()
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

    fun loadInitialDataAndFirstPage(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            loadFirstPage()
            _networkState.value = NetworkState.LOADED
            initialPageLoaded = true
        }
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
            loadFirstPage()
            _refreshState.value = NetworkState.LOADED
        }
    }

    private suspend fun loadFirstPage(){
        try {
            val response = when{
                ::listingType.isInitialized -> listingRepository.getListing(listingType, postSort, t, null, pageSize * 3, user).await()
                ::subredditWhere.isInitialized -> subredditRepository.getNetworkSubreddits(subredditWhere, null, pageSize * 3).await()
                ::messageWhere.isInitialized -> messageRepository.getMessages(messageWhere, null, pageSize * 3).await()
                else -> throw IllegalStateException("Not enough info to load listing")
            }
            val items = ArrayList(response.data.children.map { it.data })
            if(::listingType.isInitialized){
                listingRepository.getReadPosts().map { it.name }.toCollection(readItemIds)
                setItemsReadStatus(items, readItemIds)
            }
            if(::subredditWhere.isInitialized){
                if(favoriteSubsHash == null){
                    favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName }.toHashSet()
                }

                if(subscribedSubsHash == null){
                    subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName }.toHashSet()
                }
                setSubsAndFavorites(items, subscribedSubsHash!!, favoriteSubsHash!!)
            }
            _items.value = items
            lastItemReached = items.size < (pageSize * 3)
            loadedIds.clear()
            loadedIds.addAll(items.map { it.name })
            after = response.data.after
        } catch (e: Exception){
            _errorMessage.value = e.toString()
        }
    }

    fun loadAfter() {
        if(lastItemReached){
            return
        }
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            try{
                val response = when {
                    ::listingType.isInitialized -> listingRepository.getListing(listingType, postSort, t, after, pageSize, user).await()
                    ::subredditWhere.isInitialized -> subredditRepository.getNetworkSubreddits(subredditWhere, after, pageSize).await()
                    ::messageWhere.isInitialized -> messageRepository.getMessages(messageWhere, after, pageSize).await()
                    else -> throw IllegalStateException("Not enough info to load listing")
                }
                val newItems = response.data.children.map { it.data }.filter { !loadedIds.contains(it.name) }

                lastItemReached = newItems.isEmpty()
                if(lastItemReached){
                    _networkState.value = NetworkState.LOADED
                    return@launch
                }

                if(::subredditWhere.isInitialized){
                    if(favoriteSubsHash == null){
                        favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName }.toHashSet()
                    }

                    if(subscribedSubsHash == null){
                        subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName }.toHashSet()
                    }
                    setSubsAndFavorites(newItems, subscribedSubsHash!!, favoriteSubsHash!!)
                }

                loadedIds.addAll(newItems.map { it.name })
                setItemsReadStatus(newItems, readItemIds)
                _items.value!!.addAll(newItems)
                _newItems.value = newItems.toList()
                after = response.data.after
                _networkState.value = NetworkState.LOADED
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            }
        }
    }

    fun newItemsAdded(){
        _newItems.value = null
    }

}