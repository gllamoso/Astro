package dev.gtcl.reddit.ui.fragments.dialog.subreddits

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubscribeAction
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditChild
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import java.util.concurrent.Executors
import retrofit2.Callback
import retrofit2.Response

class SubredditSelectorViewModel(application: RedditApplication): AndroidViewModel(application){
    // Repos
    private val listingRepository = ListingRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun subscribe(subreddit: Subreddit, subscribeAction: SubscribeAction, favorite: Boolean){
        coroutineScope.launch {
            listingRepository.subscribe(subreddit.displayName, subscribeAction).enqueue(object: Callback<Void>{
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("TAE", "Failed") // TODO: Handle
                }

                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    coroutineScope.launch {
                        if(subscribeAction == SubscribeAction.SUBSCRIBE) insertSub(subreddit, favorite)
                        else listingRepository.removeSubreddit(subreddit)
                    }
                }
            })
        }
    }

    fun addToFavorites(subreddit: Subreddit, favorite: Boolean){
        coroutineScope.launch {
            if(favorite) {
                subscribe(subreddit, SubscribeAction.SUBSCRIBE, true)
            } else {
                listingRepository.addToFavorites(subreddit.displayName, false)
            }
        }
    }

    suspend fun insertSub(subreddit: Subreddit, favorite: Boolean){
        val sub: Subreddit = if(subreddit.name == "")
            (listingRepository.searchSubreddits(
                nsfw = true,
                includeProfiles = false,
                limit = 1,
                query = subreddit.displayName
            ).await().data.children[0] as SubredditChild).data
        else subreddit
        sub.isFavorite = favorite
        listingRepository.insertSubreddit(sub)
    }
}