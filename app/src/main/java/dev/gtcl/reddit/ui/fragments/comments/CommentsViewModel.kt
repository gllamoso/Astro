package dev.gtcl.reddit.ui.fragments.comments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.CommentSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.comments.CommentItem
import dev.gtcl.reddit.comments.More
import dev.gtcl.reddit.comments.MoreComments
import dev.gtcl.reddit.comments.convertChildrenToCommentItems
import dev.gtcl.reddit.posts.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CommentsViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val commentRepo = application.commentRepository

    // Scopes
    private var viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    var attachedToMainFragment = false

    private val _post = MutableLiveData<Post>()
    val post: LiveData<Post>
        get() = _post

    private val _comments = MutableLiveData<List<CommentItem>>()
    val comments: LiveData<List<CommentItem>>
        get() = _comments

    fun setPost(redditPost: Post){
        _post.value = redditPost
    }

    fun clearPost(){
        _post.value = null
    }

    var commentsFetched = false

    fun fetchPostAndComments(permalink: String = post.value!!.permalink){
        coroutineScope.launch {
            val commentPage = commentRepo.getPostAndComments(permalink, CommentSort.BEST).await()
            _post.value = commentPage.post
            _comments.value = commentPage.comments
        }
    }

    fun clearComments() {
        _comments.value = null
    }

    private val _moreComments = MutableLiveData<MoreComments>()
    val moreComments: LiveData<MoreComments>
        get() = _moreComments

    fun fetchMoreComments(position: Int, more: More){
        coroutineScope.launch {
            val children = commentRepo.getMoreComments(more.getChildrenAsValidString(), post.value!!.name, CommentSort.BEST).await()
            _moreComments.value = MoreComments(position, more.depth, children.convertChildrenToCommentItems(more.depth))
        }
    }

    fun clearMoreComments(){
        _moreComments.value = null
    }

    fun stopJob(){
        viewModelJob.cancel()
        viewModelJob =  Job()
        coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)
    }

    private val _scrollable = MutableLiveData<Boolean?>()
    val scrollable: LiveData<Boolean?>
        get() = _scrollable

    fun setScrollable(scrollable: Boolean?){
        _scrollable.value = scrollable
    }
}