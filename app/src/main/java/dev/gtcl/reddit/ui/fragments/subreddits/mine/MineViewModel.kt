package dev.gtcl.reddit.ui.fragments.subreddits.mine

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.database.DbMultiReddit
import dev.gtcl.reddit.models.reddit.MultiReddit
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*

class MineViewModel(private val application: RedditApplication): AndroidViewModel(application){
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _multiReddits = MutableLiveData<List<DbMultiReddit>>()
    val multiReddits: LiveData<List<DbMultiReddit>>
        get() = _multiReddits

    private val _subscribedSubs = MutableLiveData<List<Subreddit>>()
    val subscribedSubs: LiveData<List<Subreddit>>
        get() = _subscribedSubs

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _refreshState = MutableLiveData<NetworkState>()
    val refreshState: LiveData<NetworkState>
        get() = _refreshState

    fun syncWithDb(){
        coroutineScope.launch {
            _multiReddits.value = subredditRepository.getMyMultiRedditsDb()
            _subscribedSubs.value = subredditRepository.getSubscribedSubs().map { it.asDomainModel() }
        }
    }

    fun multiRedditsSynced(){
        _multiReddits.value = null
    }

    fun subredditsSynced(){
        _subscribedSubs.value = null
    }

    fun syncDbWithReddit(){
        coroutineScope.launch {
            try {
                _refreshState.value = NetworkState.LOADING
                val favSubs = subredditRepository.getFavoriteSubs().map { it.displayName }.toHashSet()
                if(application.accessToken == null) {
                    subredditRepository.deleteSubscribedSubs()
                    val subs = subredditRepository.getNetworkAccountSubreddits(100, null).await().data.children.map { it.data as Subreddit }
                    for(sub: Subreddit in subs)
                        if(favSubs.contains(sub.displayName))
                            sub.isFavorite = true
                    subredditRepository.insertSubreddits(subs)
                }
                else {
                    val allSubs = mutableListOf<Subreddit>()
                    var subs = subredditRepository.getNetworkAccountSubreddits(100, null).await().data.children.map { it.data as Subreddit }
                    while(subs.isNotEmpty()) {
                        allSubs.addAll(subs)
                        val lastSub = subs.last()
                        subs = subredditRepository.getNetworkAccountSubreddits(100, after = lastSub.name).await().data.children.map { it.data as Subreddit }
                    }
                    for(sub: Subreddit in allSubs) {
                        if (favSubs.contains(sub.displayName))
                            sub.isFavorite = true
                    }
                    subredditRepository.deleteSubscribedSubs()
                    subredditRepository.insertSubreddits(allSubs)

                    val multiReddits = subredditRepository.getMyMultiReddits().await().map { it.data }
                    subredditRepository.deleteAllMultiReddits()
                    subredditRepository.insertMultiReddits(multiReddits)

                    syncWithDb()
                }
            } catch(e: Exception) {
                _errorMessage.value = e.toString()
            } finally {
                _refreshState.value = NetworkState.LOADED
            }
        }
    }
}