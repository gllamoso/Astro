package dev.gtcl.reddit.ui.fragments.dialog.subreddits

import androidx.lifecycle.AndroidViewModel
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.listings.ListingRepository
import dev.gtcl.reddit.listings.Subreddit
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
    private val listingRepository = ListingRepository.getInstance(application, Executors.newFixedThreadPool(5))

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun addToFavorites(subreddit: Subreddit, favorite: Boolean){ // TODO: Update for multis too?
        coroutineScope.launch {
            listingRepository.addToFavorites(subreddit.displayName, favorite)
        }
    }

    fun subscribe(subreddit: Subreddit, subscribe: Boolean){
        listingRepository.subscribe(subreddit.displayName, subscribe).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) { TODO("Not yet implemented") } // TODO; Handle

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                coroutineScope.launch { listingRepository.insertSubreddit(subreddit) }
            }
        })
    }

    fun subscribe(srName: String, subscribe: Boolean){
        coroutineScope.launch {
//            listingRepository.insertSubreddit(subreddit)

        }
    }

    fun fetchSubInfoThenSubscribe(srName: String){

    }
}