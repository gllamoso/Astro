package dev.gtcl.reddit.ui.fragments.item_scroller

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.Listing
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.models.reddit.listing.checkIfItemsAreSubmittedByCurrentUser
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.MessageRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*
import kotlin.collections.HashSet

const val PAGE_SIZE = 15
class ItemScrollerVM(private val application: RedditApplication): AndroidViewModel(application){

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

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private lateinit var lastAction: () -> Unit

    private var showNsfw: Boolean = false

    private var _firstPageLoaded = false
    val firstPageLoaded: Boolean
        get() = _firstPageLoaded

    fun retry(){
        lastAction()
    }

    private lateinit var listing: Listing
    fun setListingInfo(listing: Listing, postSort: PostSort, t: Time?, pageSize: Int){
        this.listing = listing
        this.postSort = postSort
        this.t = t
        this.pageSize = pageSize
        initNsfwValue()
    }

    private lateinit var subredditWhere: SubredditWhere
    fun setListingInfo(subredditWhere: SubredditWhere, pageSize: Int){
        this.subredditWhere = subredditWhere
        this.pageSize = pageSize
        initNsfwValue()
    }

    private lateinit var messageWhere: MessageWhere
    fun setListingInfo(messageWhere: MessageWhere, pageSize: Int){
        this.messageWhere = messageWhere
        this.pageSize = pageSize
    }

    private fun initNsfwValue(){
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(application)
        val showNsfw = sharedPref.getBoolean("nsfw", true)
        val defaultSort = sharedPref.getString("default_post_sort", application.getString(R.string.order_hot))
        val sortArray = application.resources.getStringArray(R.array.post_sort_entries)
        this.showNsfw = showNsfw
    }

    fun addReadItem(item: Item){
        readItemIds.add(item.name)
        coroutineScope.launch {
            listingRepository.addReadItem(item)
        }
    }

    fun fetchFirstPage(){
        coroutineScope.launch {
            try {
                // Get listing items
                val firstPageSize = dev.gtcl.reddit.ui.fragments.listing.PAGE_SIZE * 3
                withContext(Dispatchers.IO) {
                    _firstPageLoaded = false
                    _lastItemReached.postValue(false)
                    _networkState.postValue(NetworkState.LOADING)
                    after = null

                    val firstPageItems = mutableListOf<Item>()
                    var emptyItemsCount = 0
                    while(firstPageItems.size < firstPageSize && emptyItemsCount < 3){
                        val retrieveSize = if(firstPageItems.size > (firstPageSize * 2 / 3)) dev.gtcl.reddit.ui.fragments.listing.PAGE_SIZE else firstPageSize
                        val response = when{
                            ::listing.isInitialized -> listingRepository.getListing(listing, postSort, t, after, retrieveSize, user).await()
                            ::subredditWhere.isInitialized -> subredditRepository.getSubredditsListing(subredditWhere, after, retrieveSize).await()
                            ::messageWhere.isInitialized -> messageRepository.getMessages(messageWhere, after, retrieveSize).await()
                            else -> throw IllegalStateException("Not enough info to load listing")
                        }
                        after = response.data.after

                        if(response.data.children.isNullOrEmpty()){
                            _lastItemReached.postValue(true)
                            break
                        } else {
                            val items = response.data.children.map { it.data }.filterNot { !(showNsfw) && it is Post && it.nsfw }.toMutableList().apply {
                                checkIfItemsAreSubmittedByCurrentUser(application.currentAccount?.fullId)
                            }
                            if(items.isNullOrEmpty()){
                                emptyItemsCount++
                            } else {
                                firstPageItems.addAll(items)
                            }

                            if(after == null){
                                _lastItemReached.postValue(true)
                                break
                            }
                        }
                    }

                    if(emptyItemsCount >= 3){ // Show no items if there are 3 results of empty items
                        _lastItemReached.postValue(true)
                        _items.postValue(mutableListOf())
                    } else {
                        listingRepository.getReadPosts().map { it.name }.toCollection(readItemIds)
                        setItemsReadStatus(firstPageItems, readItemIds)
                        _items.postValue(firstPageItems)
                    }

                    _networkState.postValue(NetworkState.LOADED)
                    _firstPageLoaded = true
                }
            } catch (e: Exception) {
                lastAction = ::fetchFirstPage
                after = null
                _networkState.value = NetworkState.error(e.getErrorMessage(application))
            }
        }
    }

    fun loadMore(){
        if (lastItemReached.value == true) {
            return
        }
        coroutineScope.launch {
            val previousAfter = after
            try {
                withContext(Dispatchers.IO) {
                    _networkState.postValue(NetworkState.LOADING)
                    val moreItems = mutableListOf<Item>()
                    var emptyItemsCount = 0
                    while(moreItems.size < PAGE_SIZE && emptyItemsCount < 3){
                        val response = when{
                            ::listing.isInitialized -> listingRepository.getListing(listing, postSort, t, after, PAGE_SIZE, user).await()
                            ::subredditWhere.isInitialized -> subredditRepository.getSubredditsListing(subredditWhere, after, PAGE_SIZE).await()
                            ::messageWhere.isInitialized -> messageRepository.getMessages(messageWhere, after, PAGE_SIZE).await()
                            else -> throw IllegalStateException("Not enough info to load listing")
                        }

                        after = response.data.after

                        if(response.data.children.isNullOrEmpty()){
                            _lastItemReached.postValue(true)
                            break
                        } else {
                            val items = response.data.children.map { it.data }.filterNot { !(showNsfw) && it is Post && it.nsfw }.toMutableList().apply {
                                checkIfItemsAreSubmittedByCurrentUser(application.currentAccount?.fullId)
                            }
                            if(items.isNullOrEmpty()){
                                emptyItemsCount++
                            } else {
                                moreItems.addAll(items)
                            }

                            if(after == null){
                                _lastItemReached.postValue(true)
                                break
                            }
                        }
                    }

                    if(emptyItemsCount >= 3){
                        _lastItemReached.postValue(true)
                    }

                    setItemsReadStatus(moreItems, readItemIds)
                    _moreItems.postValue(moreItems)
                    _items.value?.addAll(moreItems)
                    _networkState.postValue(NetworkState.LOADED)
                }
            } catch (e: Exception) {
                after = previousAfter
                lastAction = ::loadMore
                _networkState.value = NetworkState.error(e.getErrorMessage(application))
            }
        }
    }

    fun moreItemsObserved(){
        _moreItems.value = null
    }

    fun removeItemAt(position: Int){
        _items.value?.removeAt(position)
    }

    fun addItemAt(position: Int, item: Item){
        _items.value?.add(position, item)
    }

}