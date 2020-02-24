package dev.gtcl.reddit.ui.fragments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.*
import dev.gtcl.reddit.comments.*
import dev.gtcl.reddit.database.ReadPost
import dev.gtcl.reddit.posts.Post
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.subs.SubredditListingResponse
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
    private val _defaultSubsResult = MutableLiveData<SubredditListingResponse>()
    val defaultSubreddits: LiveData<List<Subreddit>> = Transformations.map(_defaultSubsResult) {
        it.data.children.map { child -> child.data }.sortedBy { sub -> sub.displayName.toUpperCase(
            Locale.US) }
    }

    fun fetchDefaultSubreddits(){
        coroutineScope.launch {
            try {
                _defaultSubsResult.value = subredditRepository.getSubs("default").await()
            } catch(e: Exception) {
                //TODO: Handle exception
                Log.d("TAE", "Exception: $e")
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
        repoResultsOfPopularSubreddits.value = subredditRepository.getSubs("popular", 30)
    }

    fun retryFetchPopularPosts(){
        val listing = repoResultsOfPopularSubreddits.value
        listing?.retry?.invoke()
    }
}