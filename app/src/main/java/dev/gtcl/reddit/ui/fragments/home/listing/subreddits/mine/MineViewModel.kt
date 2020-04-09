package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.mine

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.database.DbSubreddit
import dev.gtcl.reddit.database.asSubredditDomainModel
import dev.gtcl.reddit.listings.ListingRepository
import dev.gtcl.reddit.listings.Subreddit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class MineViewModel(private val application: RedditApplication): AndroidViewModel(application){

    private val repository = ListingRepository.getInstance(application, Executors.newFixedThreadPool(5))

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val dbSubs: LiveData<List<DbSubreddit>> = repository.getSubscribedSubsLive()
    val subscribedSubs: LiveData<List<Subreddit>> = Transformations.map(dbSubs) { it.asSubredditDomainModel() }

    private val dbNonFavoriteSubs: LiveData<List<DbSubreddit>> = repository.getNonFavoriteSubsLive()
    val nonFavoriteSubs: LiveData<List<Subreddit>> = Transformations.map(dbNonFavoriteSubs){ it.asSubredditDomainModel() }

    private val dbFavorites: LiveData<List<DbSubreddit>> = repository.getFavoriteSubsLive()
    val favoriteSubs: LiveData<List<Subreddit>> = Transformations.map(dbFavorites) { it.asSubredditDomainModel() }

    fun syncSubscribedSubs(){
        coroutineScope.launch {
            try {
                val favSubs = repository.getFavoriteSubs().map { it.displayName }.toHashSet()
                if(application.accessToken == null) {
                    val subs = repository.getAccountSubreddits(100, null).await().data.children.map { it.data as Subreddit }
                    for(sub: Subreddit in subs)
                        if(favSubs.contains(sub.displayName))
                            sub.isFavorite = true
                    repository.deleteSubscribedSubs()
                    repository.insertSubreddits(subs)
                }
                else {
                    val allSubs = mutableListOf<Subreddit>()
                    var subs = repository.getAccountSubreddits(100, null).await().data.children.map { it.data as Subreddit }
                    while(subs.isNotEmpty()) {
                        allSubs.addAll(subs)
                        val lastSub = subs.last()
                        subs = repository.getAccountSubreddits(100, after = lastSub.name).await().data.children.map { it.data as Subreddit }
                    }
                    for(sub: Subreddit in allSubs) {
                        if (favSubs.contains(sub.displayName))
                            sub.isFavorite = true
                    }
                    repository.deleteSubscribedSubs()
                    repository.insertSubreddits(allSubs)
                }
            } catch(e: Exception) {
                Log.d("TAE", "Exception: $e") // TODO: Handle?
            }
        }
    }
}