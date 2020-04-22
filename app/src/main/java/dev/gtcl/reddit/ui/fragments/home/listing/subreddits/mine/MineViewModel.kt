package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.mine

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.database.asDomainModel
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.models.reddit.Subreddit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MineViewModel(private val application: RedditApplication): AndroidViewModel(application){

    private val repository = ListingRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _initialSubs = MutableLiveData<List<Subreddit>>()
    val initialSubs : LiveData<List<Subreddit>>
        get() = _initialSubs

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    var refresh: Boolean = false

    fun loadInitial(){
        coroutineScope.launch {
            try{
                _initialSubs.value = repository.getSubscribedSubs().asDomainModel()
            } catch(e: Exception) {
                _errorMessage.value = "Error in loading initial values"
                Log.d(TAG, "Exception: $e")
            }
        }
    }

    fun initialLoadFinished(){
        _initialSubs.value = null
    }

    fun syncSubscribedSubs(){
        coroutineScope.launch {
            try {
                val favSubs = repository.getFavoriteSubs().map { it.displayName }.toHashSet()
                if(application.accessToken == null) {
                    val subs = repository.getNetworkAccountSubreddits(100, null).await().data.children.map { it.data as Subreddit }
                    for(sub: Subreddit in subs)
                        if(favSubs.contains(sub.displayName))
                            sub.isFavorite = true
                    repository.deleteSubscribedSubs()
                    repository.insertSubreddits(subs)
                }
                else {
                    val allSubs = mutableListOf<Subreddit>()
                    var subs = repository.getNetworkAccountSubreddits(100, null).await().data.children.map { it.data as Subreddit }
                    while(subs.isNotEmpty()) {
                        allSubs.addAll(subs)
                        val lastSub = subs.last()
                        subs = repository.getNetworkAccountSubreddits(100, after = lastSub.name).await().data.children.map { it.data as Subreddit }
                    }
                    for(sub: Subreddit in allSubs) {
                        if (favSubs.contains(sub.displayName))
                            sub.isFavorite = true
                    }
                    repository.deleteSubscribedSubs()
                    repository.insertSubreddits(allSubs)
                }
                loadInitial()
            } catch(e: Exception) {
                _errorMessage.value = "Failed to sync subscribed Subreddits"
                Log.d(TAG, "Exception: $e")
            }
        }
    }

    companion object{
        val TAG = ::MineViewModel.javaClass.simpleName
    }
}