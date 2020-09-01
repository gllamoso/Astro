package dev.gtcl.astro.ui.fragments.listing

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.repositories.reddit.ListingRepository
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.repositories.reddit.SubredditRepository
import kotlinx.coroutines.*

const val PAGE_SIZE = 15
class ListingVM(val application: AstroApplication) : AndroidViewModel(application) {

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _items = MutableLiveData<MutableList<Item>>()
    val items: LiveData<MutableList<Item>>
        get() = _items

    private val _moreItems = MutableLiveData<List<Item>?>()
    val moreItems: LiveData<List<Item>?>
        get() = _moreItems

    private val _subreddit = MutableLiveData<Subreddit?>()
    val subreddit: LiveData<Subreddit?>
        get() = _subreddit

    private lateinit var lastAction: () -> Unit

    private val readItemIds = HashSet<String>()
    private var after: String? = null

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _postSort = MutableLiveData<PostSort>()
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

    private var _showNsfw: Boolean = false
    val showNsfw: Boolean
        get() = _showNsfw

    private lateinit var _listing: Listing
    val listing: Listing
        get() = _listing

    fun retry() {
        lastAction()
    }

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

    fun setListing(listing: Listing, showNsfw: Boolean){
        _listing = listing
        _title.value = getListingTitle(application, listing)
        _showNsfw = showNsfw

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(application)
        val defaultSort = sharedPref.getString(DEFAULT_POST_SORT_KEY, application.getString(R.string.order_hot))
        val sortArray = application.resources.getStringArray(R.array.post_sort_entries)
        val postSort: PostSort
        val time: Time?
        if(listing is SearchListing){
            postSort = PostSort.RELEVANCE
            time = Time.ALL
        } else {
            when(sortArray.indexOf(defaultSort)){
                1 -> {
                    postSort = PostSort.HOT
                    time = null
                }
                2 -> {
                    postSort = PostSort.NEW
                    time = null
                }
                3 -> {
                    postSort = PostSort.RISING
                    time = null
                }
                4 -> {
                    postSort = PostSort.CONTROVERSIAL
                    time = Time.HOUR
                }
                5 -> {
                    postSort = PostSort.CONTROVERSIAL
                    time = Time.DAY
                }
                6 -> {
                    postSort = PostSort.CONTROVERSIAL
                    time = Time.WEEK
                }
                7 -> {
                    postSort = PostSort.CONTROVERSIAL
                    time = Time.MONTH
                }
                8 -> {
                    postSort = PostSort.CONTROVERSIAL
                    time = Time.YEAR
                }
                9 -> {
                    postSort = PostSort.CONTROVERSIAL
                    time = Time.ALL
                }
                10 -> {
                    postSort = PostSort.TOP
                    time = Time.HOUR
                }
                11 -> {
                    postSort = PostSort.TOP
                    time = Time.DAY
                }
                12 -> {
                    postSort = PostSort.TOP
                    time = Time.WEEK
                }
                13 -> {
                    postSort = PostSort.TOP
                    time = Time.MONTH
                }
                14 -> {
                    postSort = PostSort.TOP
                    time = Time.YEAR
                }
                15 -> {
                    postSort = PostSort.TOP
                    time = Time.ALL
                }
                else ->{
                    postSort = PostSort.BEST
                    time = null
                }
            }
        }

        setSort(postSort, time)
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
                    _lastItemReached.postValue(false)
                    _networkState.postValue(NetworkState.LOADING)
                    after = null

                    val firstPageItems = mutableListOf<Item>()
                    var emptyItemsCount = 0
                    while(firstPageItems.size < firstPageSize && emptyItemsCount < 3){
                        val retrieveSize = if(firstPageItems.size > (firstPageSize * 2 / 3)) PAGE_SIZE else firstPageSize
                        val response = listingRepository.getListing(
                            listing,
                            postSort.value!!,
                            time.value,
                            after,
                            retrieveSize
                        ).await()
                        after = response.data.after

                        if(response.data.children.isNullOrEmpty()){
                            _lastItemReached.postValue(true)
                            break
                        } else {
                            val items = response.data.children.map { it.data }.filterNot { !(showNsfw) && it is Post && it.nsfw }.toMutableList()
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

    fun loadMore() {
        if (lastItemReached.value == true || _networkState.value == NetworkState.LOADING) {
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
                            val items = response.data.children.map { it.data }.filterNot { !(showNsfw) && it is Post && it.nsfw }.toMutableList()
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

    fun updateItemAt(position: Int, item: Item) {
        _items.value?.set(position, item)
    }

}