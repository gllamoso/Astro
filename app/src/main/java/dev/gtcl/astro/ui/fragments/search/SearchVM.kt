package dev.gtcl.astro.ui.fragments.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.SubredditChild
import dev.gtcl.astro.network.NetworkState
import kotlinx.coroutines.*
import java.util.*

class SearchVM(private val application: AstroApplication) : AstroViewModel(application) {

    private val _networkState =
        MutableLiveData<NetworkState>().apply { value = NetworkState.LOADING }
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _searchItems = MutableLiveData<List<Item>>().apply { value = listOf() }
    val searchItems: LiveData<List<Item>>
        get() = _searchItems

    private val _popularItems = MutableLiveData<MutableList<Item>>().apply { value = arrayListOf() }
    val popularItems: LiveData<MutableList<Item>>
        get() = _popularItems

    private val _morePopularItems = MutableLiveData<List<Item>?>()
    val morePopularItems: LiveData<List<Item>?>
        get() = _morePopularItems

    private val _lastItemReached = MutableLiveData<Boolean>()
    val lastItemReached: LiveData<Boolean>
        get() = _lastItemReached

    private var after: String? = null
    private val pageSize = 25

    private var _firstPageLoaded = false
    val firstPageLoaded: Boolean
        get() = _firstPageLoaded

    private val _selectedItems =
        MutableLiveData<MutableSet<String>>().apply { value = mutableSetOf() }
    val selectedItems: LiveData<MutableSet<String>>
        get() = _selectedItems

    private val _isSearching = MutableLiveData<Boolean>().apply { value = false }
    val isSearching: LiveData<Boolean>
        get() = _isSearching

    private lateinit var lastAction: () -> Unit

    private val _showPopular = MutableLiveData<Boolean>().apply { value = true }
    val showPopular: LiveData<Boolean>
        get() = _showPopular

    fun showPopular(show: Boolean) {
        _showPopular.value = show
    }

    fun retry() {
        lastAction()
    }

    fun searchSubreddits(query: String) {
        coroutineScope.launch {
            _isSearching.postValue(true)
            try {
                val results = ArrayList<Item>()
                fetchAccountIfItExists(query, results)
                searchSubreddits(query, results)
                _searchItems.postValue(results)
            } catch (e: Exception) {
                _searchItems.postValue(listOf())
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isSearching.postValue(false)
            }
        }
    }

    private suspend fun fetchAccountIfItExists(query: String, results: MutableList<Item>) {
        try {
            val account = userRepository.getAccountInfo(query).await().data
            results.add(account)
        } catch (e: Exception) {
        }
    }

    private suspend fun searchSubreddits(query: String, results: MutableList<Item>) {
        val subs = subredditRepository.searchSubreddits(
            nsfw = true,
            includeProfiles = false,
            limit = 10,
            query = query
        ).await().data.children.map { (it as SubredditChild).data }
        results.addAll(subs)
    }

    fun loadPopular() {
        coroutineScope.launch {
            _networkState.postValue(NetworkState.LOADING)
            try {
                val size = pageSize * 3
                val response =
                    subredditRepository.getSubredditsListing(SubredditWhere.POPULAR, after, size)
                        .await()
                val subs = response.data.children.map { it.data }.toMutableList()
                _popularItems.postValue(subs)
                _lastItemReached.postValue(subs.size < size)
                after = response.data.after
                _networkState.postValue(NetworkState.LOADED)
                _firstPageLoaded = true
            } catch (e: Exception) {
                lastAction = ::loadPopular
                after = null
                _networkState.postValue(NetworkState.error(e.getErrorMessage(application)))
            }
        }
    }

    fun loadMorePopular() {
        coroutineScope.launch {
            val previousAfter = after
            _networkState.postValue(NetworkState.LOADING)
            try {
                val size = if (popularItems.value.isNullOrEmpty()) {
                    pageSize * 3
                } else {
                    pageSize
                }
                val response =
                    subredditRepository.getSubredditsListing(SubredditWhere.POPULAR, after, size)
                        .await()
                val subs = response.data.children.map { it.data }
                _morePopularItems.postValue(subs)
                _popularItems.value?.addAll(subs)
                _lastItemReached.postValue(subs.size < size)
                after = response.data.after
                _networkState.postValue(NetworkState.LOADED)
            } catch (e: Exception) {
                after = previousAfter
                lastAction = ::loadMorePopular
                _networkState.postValue(NetworkState.error(e.getErrorMessage(application)))
            }
        }
    }

    fun morePopularItemsObserved() {
        _morePopularItems.value = null
    }

    fun addSelectedItem(item: String) {
        _selectedItems += item
    }

    fun removeSelectedItem(item: String) {
        _selectedItems -= item
    }

}