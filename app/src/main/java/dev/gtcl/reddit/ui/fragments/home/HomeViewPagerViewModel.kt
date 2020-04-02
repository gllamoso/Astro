package dev.gtcl.reddit.ui.fragments.home

import android.util.Log
import androidx.lifecycle.*
import dev.gtcl.reddit.*
import dev.gtcl.reddit.listings.ListingItem
import dev.gtcl.reddit.listings.ListingRepository
import dev.gtcl.reddit.listings.SubredditListing
import dev.gtcl.reddit.listings.subs.Subreddit
import dev.gtcl.reddit.listings.subs.SubredditRepository
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.Executors

class HomeViewPagerViewModel(val application: RedditApplication): AndroidViewModel(application){

    // Repositories
    private val postRepository = ListingRepository.getInstance(application, Executors.newFixedThreadPool(5))
    private val subredditRepository = SubredditRepository.getInstance(Executors.newFixedThreadPool(5))

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    // Mine
    private val _defaultSubreddits = MutableLiveData<List<Subreddit>>()
    val defaultSubreddits: LiveData<List<Subreddit>> = Transformations.map(_defaultSubreddits) {
        it.sortedBy { sub -> sub.displayName.toUpperCase(Locale.US) }
    }

    fun fetchDefaultSubreddits(){
        coroutineScope.launch {
            try {
                if(application.accessToken == null)
                    _defaultSubreddits.value = subredditRepository.getSubs("default").await().data.children.map { it.data }
                else {
                    val allSubs = mutableListOf<Subreddit>()
                    var subs = subredditRepository.getSubs("subscriber", application.accessToken).await().data.children.map { it.data }
                    while(subs.isNotEmpty()) {
                        allSubs.addAll(subs)
                        val lastSub = subs.last()
                        subs = subredditRepository.getSubs("subscriber", application.accessToken, after = lastSub.name).await().data.children.map { it.data }
                    }
                    _defaultSubreddits.value = allSubs
                }
            } catch(e: Exception) {
                Log.d(TAG, "Exception: $e") // TODO: Handle?
            }
        }
    }

    // Trending
    private val _repoResultsOfTrendingSubreddits = MutableLiveData<Listing<ListingItem>>()
    val trendingSubredditPosts = Transformations.switchMap(_repoResultsOfTrendingSubreddits) { it.pagedList }

    fun fetchTrendingPosts() {
        _repoResultsOfTrendingSubreddits.value = postRepository.getPostsFromNetwork(
            SubredditListing(Subreddit(displayName = "trendingsubreddits")),
            PostSort.HOT, pageSize = 5)
    }

    // Popular
    private val repoResultsOfPopularSubreddits = MutableLiveData<Listing<Subreddit>>()
    val popularSubreddits = Transformations.switchMap(repoResultsOfPopularSubreddits) { it.pagedList }

    fun fetchPopularPosts(){
        repoResultsOfPopularSubreddits.value = subredditRepository.getSubsListing("popular", 30)
    }

    fun retryFetchPopularPosts(){
        val listing = repoResultsOfPopularSubreddits.value
        listing?.retry?.invoke()
    }

    // Search
    private val _searchSubreddits = MutableLiveData<List<Subreddit>>()
    val searchSubreddits: LiveData<List<Subreddit>>
        get() = _searchSubreddits

    fun searchForSubs(q: String, nsfw: String){
        coroutineScope.launch {
            try {
                _searchSubreddits.value = subredditRepository.getSubsSearch(q, nsfw).await().data.children.map { it.data }
            } catch(e: Exception) {
                Log.d(TAG, "Exception: $e") // TODO: Handle
            }
        }
    }

    fun clearSearchResults(){
        _searchSubreddits.value = null
    }

    private var _currentPage = 0
    val currentPage: Int
        get() = _currentPage

    fun setCurrentPage(page: Int){
        _currentPage = page
    }

    companion object {
        private val TAG = HomeViewPagerViewModel::class.qualifiedName
    }
}