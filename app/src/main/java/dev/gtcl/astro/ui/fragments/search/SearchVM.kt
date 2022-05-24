package dev.gtcl.astro.ui.fragments.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.SubredditChild
import dev.gtcl.astro.ui.fragments.item_scroller.ItemScrollerVM
import kotlinx.coroutines.launch

class SearchVM(private val application: AstroApplication) : ItemScrollerVM(application) {


    private val _searchItems = MutableLiveData<List<Item>>().apply { value = listOf() }
    val searchItems: LiveData<List<Item>>
        get() = _searchItems

    private val _selectedItems =
        MutableLiveData<MutableSet<String>>().apply { value = mutableSetOf() }
    val selectedItems: LiveData<MutableSet<String>>
        get() = _selectedItems

    private val _isSearching = MutableLiveData<Boolean>().apply { value = false }
    val isSearching: LiveData<Boolean>
        get() = _isSearching

    private val _showPopular = MutableLiveData<Boolean>().apply { value = true }
    val showPopular: LiveData<Boolean>
        get() = _showPopular

    init {
        setListingInfo(SubredditWhere.POPULAR)
        _showNsfw = application.sharedPref.getBoolean(NSFW_KEY, false)
    }

    fun showPopular(show: Boolean) {
        _showPopular.value = show
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
            nsfw = showNsfw,
            includeProfiles = false,
            limit = 10,
            query = query
        ).await().data.children.map { (it as SubredditChild).data }
            .filter { it.subredditType == "public" }
        results.addAll(subs)
    }

    fun addSelectedItem(item: String) {
        _selectedItems += item
    }

    fun removeSelectedItem(item: String) {
        _selectedItems -= item
    }

}