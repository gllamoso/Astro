package dev.gtcl.reddit.ui.main.fragments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.*
import dev.gtcl.reddit.comments.*
import dev.gtcl.reddit.database.ReadPost
import dev.gtcl.reddit.posts.RedditPost
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.subs.SubredditListingResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class MainFragmentViewModel(val application: RedditApplication): ViewModel(){

    private val postRepository = application.postRepository
    private val commentRepository = application.commentRepository
    private val subredditRepository = application.subredditRepository

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val allReadPosts = postRepository.getReadPostsFromDatabase()

    private val _subredditSelected = MutableLiveData<Subreddit>()
    val subredditSelected: LiveData<Subreddit>
        get() = _subredditSelected

    private val _sortSelected = MutableLiveData<PostSort>()
    val sortSelected: LiveData<PostSort>
        get() = _sortSelected

    private val _timeSelected = MutableLiveData<Time>()
    val timeSelected: LiveData<Time>
        get() = _timeSelected

    private val postListingsOfSubreddit = MutableLiveData<Listing<RedditPost>>()
    val posts = Transformations.switchMap(postListingsOfSubreddit) { it.pagedList }
    val networkState = Transformations.switchMap(postListingsOfSubreddit) { it.networkState }
    val refreshState = Transformations.switchMap(postListingsOfSubreddit) { it.refreshState }

    private val _dismissDialog = MutableLiveData<Boolean>()
    val dismissDialog: LiveData<Boolean>
        get() = _dismissDialog

    private val _navigateToSubredditSelection = MutableLiveData<Any>()
    val navigateToSubredditSelection: LiveData<Any>
        get() = _navigateToSubredditSelection

    private val _selectedPost = MutableLiveData<RedditPost>()
    val selectedPost: LiveData<RedditPost>
        get() = _selectedPost

    private val _commentListing = MutableLiveData<List<Comment>>()
    val commentListing: LiveData<List<Comment>>
        get() = _commentListing

    private val _currentPage = MutableLiveData<Int?>()
    val currentPage: LiveData<Int?>
        get() = _currentPage

    private val _scrollable = MutableLiveData<Boolean?>()
    val scrollable: LiveData<Boolean?>
        get() = _scrollable

    fun selectPost(post: RedditPost){
        _selectedPost.value = post
    }

    fun addReadPost(readPost: ReadPost) {
        coroutineScope.launch {
            postRepository.insertReadPostToDatabase(readPost)
        }
    }

    fun refresh() = postListingsOfSubreddit.value?.refresh?.invoke()

    fun retry() {
        val listing = postListingsOfSubreddit.value
        listing?.retry?.invoke()
    }

    fun getPosts(subreddit: Subreddit?, sortBy: PostSort = PostSort.HOT, timePeriod: Time? = null): Boolean{
        if(subredditSelected.value?.displayName == subreddit?.displayName && sortSelected.value == sortBy && timeSelected.value == timePeriod)
            return false

        _subredditSelected.value = subreddit
        _sortSelected.value = sortBy
        _timeSelected.value = timePeriod
        postListingsOfSubreddit.value = postRepository.getPostsOfSubreddit(subreddit!!, sortBy, timePeriod, 30)
        return true
    }

    //-----------------COMMENTS-----------------------------

    private val _redditPost = MutableLiveData<RedditPost>()
    val redditPost: LiveData<RedditPost>
        get() = _redditPost

    private val _comments = MutableLiveData<List<CommentItem>>()
    val comments: LiveData<List<CommentItem>>
        get() = _comments

    private val _moreComments = MutableLiveData<MoreComments>()
    val moreComments: LiveData<MoreComments>
        get() = _moreComments

    fun setPost(redditPost: RedditPost){
        _redditPost.value = redditPost
    }

    fun getPostAndComments(permalink: String = redditPost.value!!.permalink){
        coroutineScope.launch {
            val commentPage = commentRepository.getPostAndComments(permalink, CommentSort.BEST).await()
            _redditPost.value = commentPage.post
            _comments.value = commentPage.comments
        }
    }

    fun getMoreComments(position: Int, more: More){
        coroutineScope.launch {
            val children = commentRepository.getMoreComments(more.getChildrenAsValidString(), redditPost.value!!.name, CommentSort.BEST).await()
            _moreComments.value = MoreComments(position, more.depth, children.convertChildrenToCommentItems(more.depth))
        }
    }

    fun clearMoreComments(){
        _moreComments.value = null
    }

    fun clearComments() {
        _comments.value = null
    }

    fun scrollToPage(position: Int?){
        _currentPage.value = position
    }

    fun setScrollable(scrollable: Boolean?){
        _scrollable.value = scrollable
    }

    //-----------------SUBREDDIT SELECTOR-----------------------------
    // Mine
    private val _defaultSubsResult = MutableLiveData<SubredditListingResponse>()
    val defaultSubreddits: LiveData<List<Subreddit>> = Transformations.map(_defaultSubsResult) {
        it.data.children.map { child -> child.data }.sortedBy { sub -> sub.displayName.toUpperCase(
            Locale.US) }
    }

    fun getDefaultSubreddits(){
        coroutineScope.launch {
//            var results = if(accessToken != null) {
//                val accessTokenVal = accessToken
//                subredditRepository.getSubsOfMine("subscriber", accessTokenVal!!)
//            } else
            val results = subredditRepository.getSubs("default")
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
}