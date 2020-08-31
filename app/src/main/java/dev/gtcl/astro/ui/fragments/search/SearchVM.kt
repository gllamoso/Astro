package dev.gtcl.astro.ui.fragments.search

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.SubredditWhere
import dev.gtcl.astro.minusAssign
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.SubredditChild
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.plusAssign
import dev.gtcl.astro.repositories.reddit.SubredditRepository
import dev.gtcl.astro.repositories.reddit.UserRepository
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.util.*

class SearchVM(application: AstroApplication) : AndroidViewModel(application){
    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _networkState = MutableLiveData<NetworkState>().apply { value = NetworkState.LOADING }
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _searchItems = MutableLiveData<List<Item>>().apply { value = listOf() }
    val searchItems: LiveData<List<Item>>
        get() = _searchItems

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

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
    var initialPageLoaded = false

    private val _selectedItems = MutableLiveData<MutableSet<String>>().apply { value = mutableSetOf() }
    val selectedItems: LiveData<MutableSet<String>>
        get() = _selectedItems

    fun retry(){
        TODO("Need to be able to retry failed network requests")
    }

    fun searchSubreddits(query: String){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            try {
                val results = ArrayList<Item>()
                fetchAccountIfItExists(query, results)
                searchSubreddits(query, results)
                _searchItems.value = results
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            } finally {
                _networkState.value = NetworkState.LOADED
            }
        }
    }

    private suspend fun fetchAccountIfItExists(query: String, results: MutableList<Item>){
        try {
            val account = userRepository.getAccountInfo(query).await().data
            results.add(account)
        } catch (e: HttpException){
            if(e.code() == 404){
                Log.i("Search", "Account not found: ${e.code()}")
            } else {
                throw e
            }
        }
    }

    private suspend fun searchSubreddits(query: String, results: MutableList<Item>){
        val subs = subredditRepository.searchSubreddits(
            nsfw = true,
            includeProfiles = false,
            limit = 10,
            query = query
        ).await().data.children.map { (it as SubredditChild).data }
        results.addAll(subs)
    }

    fun loadPopular(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            try {
                val size = pageSize * 3
                val response = subredditRepository.getSubredditsListing(SubredditWhere.POPULAR, after, size).await()
                val subs = response.data.children.map { it.data }.toMutableList()
                _popularItems.value = subs
                _lastItemReached.value = subs.size < size
                after = response.data.after
            } catch(e: Exception){
                _errorMessage.value = e.toString()
            } finally {
                _networkState.value = NetworkState.LOADED
            }
        }
    }

    fun loadMorePopular(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            try {
                val size = if(popularItems.value.isNullOrEmpty()){
                    pageSize * 3
                } else {
                    pageSize
                }
                val response = subredditRepository.getSubredditsListing(SubredditWhere.POPULAR, after, size).await()
                val subs = response.data.children.map { it.data }
                _morePopularItems.value = subs
                _popularItems.value?.addAll(subs)
                _lastItemReached.value = subs.size < size
                after = response.data.after
            } catch(e: Exception){
                _errorMessage.value = e.toString()
            } finally {
                _networkState.value = NetworkState.LOADED
            }
        }
    }

    fun morePopularItemsObserved(){
        _morePopularItems.value = null
    }

    fun addSelectedItem(item: String){
        _selectedItems += item
    }

    fun removeSelectedItem(item: String){
        _selectedItems -= item
    }

}