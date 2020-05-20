package dev.gtcl.reddit.ui.fragments.subreddits

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubscribeAction
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditChild
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SubredditSelectorViewModel(private val application: RedditApplication): AndroidViewModel(application){
    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    fun subscribe(subreddit: Subreddit, subscribeAction: SubscribeAction, favorite: Boolean){
        coroutineScope.launch {
            subredditRepository.subscribe(subreddit.displayName, subscribeAction).enqueue(object: Callback<Void>{
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    _errorMessage.value = t.localizedMessage
                }

                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    coroutineScope.launch {
                        if(subscribeAction == SubscribeAction.SUBSCRIBE) {
                            insertSub(subreddit, favorite)
                        }
                        else {
                            subredditRepository.removeSubreddit(subreddit)
                        }
                    }
                }
            })
        }
    }

    fun addToFavorites(subreddit: Subreddit, favorite: Boolean){
        coroutineScope.launch {
            if(favorite) {
                subscribe(subreddit, SubscribeAction.SUBSCRIBE, favorite)
            } else {
                subredditRepository.addToFavorites(subreddit.displayName, favorite)
            }
        }
    }

    suspend fun insertSub(subreddit: Subreddit, favorite: Boolean){
        val sub: Subreddit = if(subreddit.name == "")
            (subredditRepository.searchSubreddits(
                nsfw = true,
                includeProfiles = false,
                limit = 1,
                query = subreddit.displayName
            ).await().data.children[0] as SubredditChild).data
        else subreddit
        sub.isFavorite = favorite
        subredditRepository.insertSubreddit(sub)
    }

    fun syncSubscribedSubsAndMultiReddits(){
        coroutineScope.launch {
            try {
                val favSubs = subredditRepository.getFavoriteSubs().map { it.displayName }.toHashSet()
                if(application.accessToken == null) {
                    val subs = subredditRepository.getNetworkAccountSubreddits(100, null).await().data.children.map { it.data as Subreddit }
                    for(sub: Subreddit in subs)
                        if(favSubs.contains(sub.displayName))
                            sub.isFavorite = true
                    subredditRepository.deleteSubscribedSubs()
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
                }
            } catch(e: Exception) {
                _errorMessage.value = e.toString()
            }
        }
    }
}