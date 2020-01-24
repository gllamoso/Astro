package dev.gtcl.reddit.ui.subreddits

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.posts.RedditPost
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.Listing
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.subs.SubredditListingResponse
import kotlinx.coroutines.*
import java.util.*

class SubredditSelectorViewModel(application: RedditApplication): ViewModel() {
    private val postRepository = application.postRepository
    private val subredditRepository = application.subredditRepository

    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(
        viewModelJob + Dispatchers.Main)

    private var accessToken: String? = null

    private val _subredditSelection = MutableLiveData<Subreddit>()
    val subredditSelection: LiveData<Subreddit>
        get() = _subredditSelection

    fun selectSubreddit(subreddit: Subreddit?){
        _subredditSelection.value = subreddit
    }

    fun subredditSelected(){
        _subredditSelection.value = null
    }

    // Mine
    private val _defaultSubsResult = MutableLiveData<SubredditListingResponse>()
    val defaultSubreddits: LiveData<List<Subreddit>> = Transformations.map(_defaultSubsResult) {
        it.data.children.map { child -> child.data }.sortedBy { sub -> sub.displayName.toUpperCase(Locale.US) }
    }

    fun getDefaultSubreddits(){
        coroutineScope.launch {
            var results = if(accessToken != null) {
                    val accessTokenVal = accessToken
                    subredditRepository.getSubsOfMine("subscriber", accessTokenVal!!)
                } else
                    subredditRepository.getSubs("default")
            try {
                _defaultSubsResult.postValue(results.await())
            } catch(e: Exception) {
                //TODO: Handle exception
                Log.d("TAE", "Exception: $e")
            }
        }
    }

    // Trending
    private val _repoResultsOfTrendingSubreddits = MutableLiveData<Listing<RedditPost>>()
    val trendingSubredditPosts = Transformations.switchMap(_repoResultsOfTrendingSubreddits) { it.pagedList }

    fun getTrendingPosts() {
        _repoResultsOfTrendingSubreddits.value = postRepository.getPostsOfSubreddit(
            Subreddit(
                displayName = "trendingsubreddits"
            ), PostSort.HOT, pageSize = 5)
    }

    // Popular
    private val repoResultsOfPopularSubreddits = MutableLiveData<Listing<Subreddit>>()
    val popularSubreddits = Transformations.switchMap(repoResultsOfPopularSubreddits) { it.pagedList }

    fun getPopularPosts(){
        repoResultsOfPopularSubreddits.value = subredditRepository.getSubs("popular", 30)
    }

    fun retryPopular(){
        val listing = repoResultsOfPopularSubreddits.value
        listing?.retry?.invoke()
    }

    fun setAccessToken(accessToken: String?){
        this.accessToken = accessToken
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("TAE", "SubredditSelectorViewModel cleared")
    }
}