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

const val PAGE_SIZE = 15
class ListingVM(val application: RedditApplication) : AndroidViewModel(application) {

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

    private lateinit var lastAction: () -> Unit

    fun retry() {
        lastAction()
    }

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

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _postSort = MutableLiveData<PostSort>().apply { value = PostSort.HOT }
    val postSort: LiveData<PostSort>
        get() = _postSort

    private val _time = MutableLiveData<Time?>().apply { value = null }
    val time: LiveData<Time?>
        get() = _time

    private var _firstPageLoaded = false
    val firstPageLoaded: Boolean
        get() = _firstPageLoaded

    private val _lastItemReached = MutableLiveData<Boolean>().apply { value = false }
    val lastItemReached: LiveData<Boolean>
        get() = _lastItemReached

    private val _leftDrawerExpanded = MutableLiveData<Boolean>()
    val leftDrawerExpanded: LiveData<Boolean>
        get() = _leftDrawerExpanded

    var showNsfw: Boolean = false

    private lateinit var _listing: Listing
    val listing: Listing
        get() = _listing

    fun fetchSubreddit(displayName: String){
        coroutineScope.launch {
            try{
                val sub = subredditRepository.getSubreddit(displayName)
                    .await().data
                _subreddit.value = sub
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun setSubreddit(sub: Subreddit?){
        _subreddit.value = sub
    }

    fun setListing(listing: Listing){
        _listing = listing
        _title.value = getListingTitle(application, listing)
    }

    fun setSort(postSort: PostSort, time: Time? = null) {
        _postSort.value = postSort
        _time.value = time
    }

    fun fetchFirstPage() {
        coroutineScope.launch {
            try {
                // Get listing items
                val firstPageSize = PAGE_SIZE * 3
                withContext(Dispatchers.IO) {
                    _firstPageLoaded = false
                    _networkState.postValue(NetworkState.LOADING)
                    after = null

                    val firstPageItems = mutableListOf<Item>()
                    var emptyItemsCount = 0
                    while(firstPageItems.size < firstPageSize && emptyItemsCount < 3){
                        val response = listingRepository.getListing(
                            listing,
                            postSort.value!!,
                            time.value,
                            after,
                            if(firstPageItems.size > (firstPageSize * 2 / 3)) PAGE_SIZE else firstPageSize
                        ).await()
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

    fun loadMore() {
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
                        val response = listingRepository.getListing(
                            listing,
                            postSort.value!!,
                            time.value,
                            after,
                            PAGE_SIZE
                        ).await()

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

    fun moreItemsObserved() {
        _moreItems.value = null
    }

    fun removeItemAt(position: Int) {
        _items.value?.removeAt(position)
    }

    fun addReadItem(item: Item) {
        readItemIds.add(item.name)
        coroutineScope.launch {
            listingRepository.addReadItem(item)
        }
    }

    fun updateItem(item: Item, position: Int) {
        _items.updateItem(item, position)
    }

    fun toggleLeftDrawerExpanding() {
        _leftDrawerExpanded.value = !(_leftDrawerExpanded.value!!)
    }

    fun setLeftDrawerExpanded(expand: Boolean) {
        _leftDrawerExpanded.value = expand
    }

}