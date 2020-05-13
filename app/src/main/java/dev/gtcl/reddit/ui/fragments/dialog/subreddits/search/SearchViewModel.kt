package dev.gtcl.reddit.ui.fragments.dialog.subreddits.search

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditChild
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchViewModel(application: RedditApplication) : AndroidViewModel(application){
    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val subscribedSubs = subredditRepository.getSubscribedSubsLive()

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _searchedSubreddits = MutableLiveData<List<Subreddit>>()
    val searchedSubreddits: LiveData<List<Subreddit>>
        get() = _searchedSubreddits

    fun searchSubreddits(query: String){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            _searchedSubreddits.value = subredditRepository.searchSubreddits(
                nsfw = true,
                includeProfiles = false,
                limit = 20,
                query = query
            ).await().data.children.map { (it as SubredditChild).data }
            _networkState.value = NetworkState.LOADED
        }
    }

    fun searchComplete(){
        _searchedSubreddits.value = null
    }
}