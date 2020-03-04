package dev.gtcl.reddit.ui.fragments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.*
import dev.gtcl.reddit.posts.Post
import dev.gtcl.reddit.subs.Subreddit
import kotlinx.coroutines.*
import java.util.*

class MainFragmentViewModel(val application: RedditApplication): ViewModel(){

    // Repositories
    private val postRepository = application.postRepository
    private val subredditRepository = application.subredditRepository

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
    private val _repoResultsOfTrendingSubreddits = MutableLiveData<Listing<Post>>()
    val trendingSubredditPosts = Transformations.switchMap(_repoResultsOfTrendingSubreddits) { it.pagedList }

    fun fetchTrendingPosts() {
        _repoResultsOfTrendingSubreddits.value = postRepository.getPostsOfSubreddit(
            Subreddit(
                displayName = "trendingsubreddits"
            ), PostSort.HOT, pageSize = 5)
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
                Log.d(MainFragmentViewModel.TAG, "Exception: $e") // TODO: Handle?
            }
        }
    }

    fun clearSearchResults(){
        _searchSubreddits.value = null
    }

    companion object {
        private val TAG = MainFragmentViewModel::class.qualifiedName
    }
}