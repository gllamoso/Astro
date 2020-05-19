package dev.gtcl.reddit.ui.fragments.subreddits.search

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditChild
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class SearchViewModel(application: RedditApplication) : AndroidViewModel(application){
    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private var favoriteSubsHash: HashSet<String>? = null
    private var subscribedSubsHash: HashSet<String>? = null

    private val _subscribedSubs = MutableLiveData<HashSet<String>>()
    val subscribedSubs: LiveData<HashSet<String>>
        get() = _subscribedSubs

    private val _favoriteSubs = MutableLiveData<HashSet<String>>()
    val favoriteSubs: LiveData<HashSet<String>>
        get() = _favoriteSubs

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _searchedSubreddits = MutableLiveData<List<Subreddit>>()
    val searchedSubreddits: LiveData<List<Subreddit>>
        get() = _searchedSubreddits

    fun syncWithDb(){
        coroutineScope.launch {
            favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName }.toHashSet()
            _favoriteSubs.value = favoriteSubsHash
            subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName }.toHashSet()
            _subscribedSubs.value = subscribedSubsHash
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
             val subs = subredditRepository.searchSubreddits(
                nsfw = true,
                includeProfiles = false,
                limit = 20,
                query = query
            ).await().data.children.map { (it as SubredditChild).data }

            if(favoriteSubsHash == null){
                favoriteSubsHash = subredditRepository.getFavoriteSubs().map { it.displayName }.toHashSet()
            }

            if(subscribedSubsHash == null){
                subscribedSubsHash = subredditRepository.getSubscribedSubs().map { it.displayName }.toHashSet()
            }

            for(item: Item in subs){
                if(item is Subreddit){
                    item.isFavorite = favoriteSubsHash!!.contains(item.displayName)
                    item.userSubscribed = subscribedSubsHash!!.contains(item.displayName)
                }
            }
            _searchedSubreddits.value = subs

            _networkState.value = NetworkState.LOADED
        }
    }

    fun searchComplete(){
        _searchedSubreddits.value = null
    }
}