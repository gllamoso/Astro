package dev.gtcl.reddit.ui.fragments.subreddits.search

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditChild
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import dev.gtcl.reddit.setSubsAndFavorites
import kotlinx.coroutines.*
import java.util.*

class SearchVM(application: RedditApplication) : AndroidViewModel(application){
    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private var favoriteSubsHash: HashSet<String>? = null
    private var subscribedSubsHash: HashSet<String>? = null

    private val _subscribedSubs = MutableLiveData<HashSet<String>?>()
    val subscribedSubs: LiveData<HashSet<String>?>
        get() = _subscribedSubs

    private val _favoriteSubs = MutableLiveData<HashSet<String>?>()
    val favoriteSubs: LiveData<HashSet<String>?>
        get() = _favoriteSubs

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _searchedSubreddits = MutableLiveData<List<Subreddit>?>()
    val searchedSubreddits: LiveData<List<Subreddit>?>
        get() = _searchedSubreddits

    fun syncWithDb(){
        coroutineScope.launch {
            withContext(Dispatchers.Default){
//                favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName }.toHashSet()
                _favoriteSubs.postValue(favoriteSubsHash)
//                subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName }.toHashSet()
                _subscribedSubs.postValue(subscribedSubsHash)
            }
        }
    }

    fun favoriteSubsSynced(){
        _favoriteSubs.value = null
    }

    fun subredditsSynced(){
        _subscribedSubs.value = null
    }

    fun searchSubreddits(query: String){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
             val subs = subredditRepository.searchSubredditsFromReddit(
                nsfw = true,
                includeProfiles = false,
                limit = 20,
                query = query
            ).await().data.children.map { (it as SubredditChild).data }

            if(favoriteSubsHash == null){
//                favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName }.toHashSet()
            }

            if(subscribedSubsHash == null){
//                subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName }.toHashSet()
            }

            setSubsAndFavorites(subs, subscribedSubsHash!!, favoriteSubsHash!!)
            _searchedSubreddits.value = subs

            _networkState.value = NetworkState.LOADED
        }
    }

    fun searchComplete(){
        _searchedSubreddits.value = null
    }
}