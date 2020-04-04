package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.mine

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.listings.ListingRepository
import dev.gtcl.reddit.listings.Subreddit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executors

class MineViewModel(private val application: RedditApplication): AndroidViewModel(application){

    private val repository = ListingRepository.getInstance(application, Executors.newFixedThreadPool(5))

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _subscribedSubs = MutableLiveData<List<Subreddit>>()
    val subscribedSubs: LiveData<List<Subreddit>> = Transformations.map(_subscribedSubs) {
        it.sortedBy { sub -> sub.displayName.toUpperCase(Locale.US) }
    }

    fun fetchSubscribedSubs(){
        coroutineScope.launch {
            try {
                if(application.accessToken == null)
                    _subscribedSubs.value = repository.getAccountSubreddit(100, null).await().data.children.map { it.data as Subreddit}
                else {
                    val allSubs = mutableListOf<Subreddit>()
                    var subs = repository.getAccountSubreddit(100, null).await().data.children.map { it.data as Subreddit }
                    while(subs.isNotEmpty()) {
                        allSubs.addAll(subs)
                        val lastSub = subs.last()
                        subs = repository.getAccountSubreddit(100, after = lastSub.name).await().data.children.map { it.data as Subreddit }
                    }
                    _subscribedSubs.value = allSubs
                }
            } catch(e: Exception) {
                Log.d("TAE", "Exception: $e") // TODO: Handle?
            }
        }
    }
}