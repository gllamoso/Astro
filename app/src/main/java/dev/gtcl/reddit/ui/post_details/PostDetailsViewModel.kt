package dev.gtcl.reddit.ui.post_details

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.CommentSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.comments.*
import dev.gtcl.reddit.posts.RedditPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PostDetailsViewModel(application: RedditApplication): ViewModel() {
    private val commentRepository = application.commentRepository

    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(
        viewModelJob + Dispatchers.Main)

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
}