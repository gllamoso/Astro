package dev.gtcl.reddit.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.CommentSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.comments.Comment
import dev.gtcl.reddit.database.ReadPost
import dev.gtcl.reddit.posts.RedditPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PostListViewModel(val application: RedditApplication): ViewModel() {

    private val postRepository = application.postRepository
    private val commentRepository = application.commentRepository

    val allReadPosts = postRepository.getReadPostsFromDatabase()

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _dismissDialog = MutableLiveData<Boolean>()
    val dismissDialog: LiveData<Boolean>
        get() = _dismissDialog

    private val _navigateToSubredditSelection = MutableLiveData<Any>()
    val navigateToSubredditSelection: LiveData<Any>
        get() = _navigateToSubredditSelection

    private val _navigateToPostDetails = MutableLiveData<RedditPost>()
    val navigateToPostDetails: LiveData<RedditPost>
        get() = _navigateToPostDetails

    private val _commentListing = MutableLiveData<List<Comment>>()
    val commentListing: LiveData<List<Comment>>
        get() = _commentListing

    fun displaySubredditSelector(){
        _navigateToSubredditSelection.value = Any()
    }

    fun displaySubredditSelectorComplete(){
        _navigateToSubredditSelection.value = null
    }

    fun selectPost(post: RedditPost){
        _navigateToPostDetails.value = post
    }

    fun postSelectionCompleted(){
        _navigateToPostDetails.value = null
    }

    fun addReadPost(readPost: ReadPost) {
        coroutineScope.launch {
            postRepository.insertReadPostToDatabase(readPost)
        }
    }
}