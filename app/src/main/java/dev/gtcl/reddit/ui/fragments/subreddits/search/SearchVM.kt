package dev.gtcl.reddit.ui.fragments.subreddits.search

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubscriptionType
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditChild
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import dev.gtcl.reddit.repositories.UserRepository
import dev.gtcl.reddit.setSubs
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.util.*

class SearchVM(application: RedditApplication) : AndroidViewModel(application){
    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _searchItems = MutableLiveData<List<Item>?>()
    val searchItems: LiveData<List<Item>?>
        get() = _searchItems

    fun searchSubreddits(query: String){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            val results = ArrayList<Item>()
            fetchAccountIfItExists(query, results)
            fetchSubreddits(query, results)
            _searchItems.value = results
            _networkState.value = NetworkState.LOADED
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

    private suspend fun fetchSubreddits(query: String, results: MutableList<Item>){
        val subs = subredditRepository.searchSubreddits(
            nsfw = true,
            includeProfiles = false,
            limit = 10,
            query = query
        ).await().data.children.map { (it as SubredditChild).data }
        results.addAll(subs)
    }

    fun searchComplete(){
        _searchItems.value = null
    }

}