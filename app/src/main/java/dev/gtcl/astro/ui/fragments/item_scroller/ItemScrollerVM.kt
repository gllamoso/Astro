package dev.gtcl.astro.ui.fragments.item_scroller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.PostListing
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.models.reddit.listing.SearchListing
import dev.gtcl.astro.network.NetworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

open class ItemScrollerVM(private val application: AstroApplication) : AstroViewModel(application) {

    private val pageSize = 25

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

    private val _postSort = MutableLiveData<PostSort>()
    val postSort: LiveData<PostSort>
        get() = _postSort

    private val _time = MutableLiveData<Time?>().apply { value = null }
    val time: LiveData<Time?>
        get() = _time

    private var _initialPageLoaded = false
    val initialPageLoaded: Boolean
        get() = _initialPageLoaded

    private val _lastItemReached = MutableLiveData<Boolean>().apply { value = false }
    val lastItemReached: LiveData<Boolean>
        get() = _lastItemReached

    private lateinit var lastAction: () -> Unit

    private var _showNsfw: Boolean = false
    val showNsfw: Boolean
        get() = _showNsfw

    private val currentItemIds = HashSet<String>()

    var postListing: PostListing? = null
    private var subredditWhere: SubredditWhere? = null
    private var messageWhere: MessageWhere? = null

    private var count = 0

    init {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(application)
        _showNsfw = sharedPref.getBoolean(NSFW_KEY, false)
        val defaultSort =
            sharedPref.getString(DEFAULT_POST_SORT_KEY, application.getString(R.string.order_hot))
        val sortArray = application.resources.getStringArray(R.array.post_sort_entries)
        val postSort: PostSort
        val time: Time?
        if (postListing is SearchListing) {
            postSort = PostSort.RELEVANCE
            time = Time.ALL
        } else {
            when (sortArray.indexOf(defaultSort)) {
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
                else -> {
                    postSort = PostSort.BEST
                    time = null
                }
            }
        }
        setListingSort(postSort, time)
    }

    fun retry() {
        lastAction()
    }

    open fun setListingInfo(
        postListing: PostListing
    ) {
        this.postListing = postListing
    }

    fun setListingSort(
        postSort: PostSort,
        time: Time? = null
    ) {
        _postSort.value = postSort
        _time.value = time
    }

    fun setListingInfo(subredditWhere: SubredditWhere) {
        this.subredditWhere = subredditWhere
    }

    fun setListingInfo(messageWhere: MessageWhere) {
        this.messageWhere = messageWhere
    }

    fun setNsfw(showNsfw: Boolean) {
        _showNsfw = showNsfw
    }

    fun addReadItem(item: Item) {
        readItemIds.add(item.name)
        coroutineScope.launch {
            try {
                miscRepository.addReadItem(item)
            } catch (e: Exception) {
                Timber.tag(this@ItemScrollerVM.javaClass.simpleName).e(e.toString())
                _errorMessage.postValue(e.toString())
            }
        }
    }

    fun fetchFirstPage() {
        coroutineScope.launch {
            try {
                // Get listing items
                val firstPageSize = pageSize * 3
                withContext(Dispatchers.IO) {
                    _lastItemReached.postValue(false)
                    _networkState.postValue(NetworkState.LOADING)
                    after = null
                    count = 0

                    val firstPageItems = mutableListOf<Item>()
                    var attempts = 0
                    val maxAttempts = 3
                    while (firstPageItems.size < firstPageSize && attempts < maxAttempts) {
                        val retrieveSize =
                            if (firstPageItems.size > (firstPageSize * 2 / 3)) pageSize else firstPageSize
                        val response = when {
                            postListing != null -> listingRepository.getPostListing(
                                postListing ?: return@withContext,
                                postSort.value ?: return@withContext,
                                time.value,
                                after,
                                retrieveSize,
                                count
                            ).await()
                            subredditWhere != null -> listingRepository.getSubredditsListing(
                                subredditWhere ?: return@withContext,
                                after,
                                retrieveSize
                            ).await()
                            messageWhere != null -> listingRepository.getMessages(
                                messageWhere ?: return@withContext,
                                after,
                                retrieveSize
                            ).await()
                            else -> throw IllegalStateException("Not enough info to load listing")
                        }
                        after = response.data.after
                        count += response.data.children.size

                        if (response.data.children.isNullOrEmpty()) {
                            _lastItemReached.postValue(true)
                            break
                        } else {
                            val items = response.data.children.map { it.data }
                                .filterNot { !(showNsfw) && it is Post && it.nsfw }.toMutableList()
                            if (items.isNullOrEmpty()) {
                                attempts++
                            } else {
                                firstPageItems.addAll(items)
                            }

                            if (after == null) {
                                _lastItemReached.postValue(true)
                                break
                            }
                        }
                    }

                    if (attempts >= maxAttempts && firstPageItems.isEmpty()) { // Show no items if there are 3 results of empty items
                        _lastItemReached.postValue(true)
                        _items.postValue(firstPageItems)
                    } else {
                        miscRepository.getReadPosts().map { it.name }.toCollection(readItemIds)
                        setItemsReadStatus(firstPageItems, readItemIds)
                        _items.postValue(firstPageItems)
                    }

                    currentItemIds.clear()
                    currentItemIds.addAll(firstPageItems.map { it.name })
                    _networkState.postValue(NetworkState.LOADED)
                    _initialPageLoaded = true
                }
            } catch (e: Exception) {
                lastAction = ::fetchFirstPage
                after = null
                count = 0
                _networkState.postValue(NetworkState.error(e.getErrorMessage(application)))
            }
        }
    }

    fun loadMore() {
        if (lastItemReached.value == true || _networkState.value == NetworkState.LOADING) {
            return
        }
        coroutineScope.launch {
            val previousAfter = after
            val previousCount = count
            try {
                withContext(Dispatchers.IO) {
                    _networkState.postValue(NetworkState.LOADING)
                    val moreItems = mutableListOf<Item>()
                    var attempts = 0
                    val maxAttempts = 3
                    while (moreItems.size < this@ItemScrollerVM.pageSize && attempts < maxAttempts) {
                        val response = when {
                            postListing != null -> listingRepository.getPostListing(
                                postListing ?: return@withContext,
                                postSort.value ?: return@withContext,
                                time.value,
                                after,
                                this@ItemScrollerVM.pageSize,
                                count
                            ).await()
                            subredditWhere != null -> listingRepository.getSubredditsListing(
                                subredditWhere ?: return@withContext,
                                after,
                                this@ItemScrollerVM.pageSize
                            ).await()
                            messageWhere != null -> listingRepository.getMessages(
                                messageWhere ?: return@withContext,
                                after,
                                this@ItemScrollerVM.pageSize
                            ).await()
                            else -> throw IllegalStateException("Not enough info to load listing")
                        }

                        after = response.data.after
                        count += response.data.children.size

                        if (response.data.children.isNullOrEmpty()) {
                            _lastItemReached.postValue(true)
                            break
                        } else {
                            val items = response.data.children.map { it.data }
                                .filterNot { currentItemIds.contains(it.name) || !(showNsfw) && it is Post && it.nsfw }
                                .toMutableList()
                            if (items.isNullOrEmpty()) {
                                attempts++
                            } else {
                                moreItems.addAll(items)
                                currentItemIds.addAll(items.map { it.name })
                            }

                            if (after == null) {
                                if (items.isEmpty()) {
                                    _lastItemReached.postValue(true)
                                    break
                                } else {
                                    val lastIndex = response.data.children.lastIndex
                                    after = response.data.children[lastIndex].data.name
                                }
                            }
                        }
                    }

                    if (attempts >= maxAttempts && moreItems.isEmpty()) {
                        _lastItemReached.postValue(true)
                    }

                    setItemsReadStatus(moreItems, readItemIds)
                    _moreItems.postValue(moreItems)
                    _items.value?.addAll(moreItems)
                    _networkState.postValue(NetworkState.LOADED)
                }
            } catch (e: Exception) {
                Timber.tag(this@ItemScrollerVM.javaClass.simpleName).e(e.toString())
                after = previousAfter
                count = previousCount
                lastAction = ::loadMore
                _networkState.postValue(NetworkState.error(e.getErrorMessage(application)))
            }
        }
    }

    fun moreItemsObserved() {
        _moreItems.value = null
    }

    fun removeItemAt(position: Int) {
        _items.value?.removeAt(position)
    }

    fun updateItemAt(position: Int, item: Item) {
        _items.value?.set(position, item)
    }
}