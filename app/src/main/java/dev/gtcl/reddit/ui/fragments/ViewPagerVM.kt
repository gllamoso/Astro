package dev.gtcl.reddit.ui.fragments

import androidx.lifecycle.*
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditChild
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViewPagerVM(private val application: RedditApplication): AndroidViewModel(application){

    // Repositories
    private val listingRepository = ListingRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    var isViewPagerSwipeEnabled = false
    var pages: ArrayList<PageType>? = null
}