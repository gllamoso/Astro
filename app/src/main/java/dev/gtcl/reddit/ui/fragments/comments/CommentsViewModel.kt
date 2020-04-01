package dev.gtcl.reddit.ui.fragments.comments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.CommentSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.listings.comments.MoreComments
import dev.gtcl.reddit.listings.comments.convertChildrenToCommentItems
import dev.gtcl.reddit.listings.ListingItem
import dev.gtcl.reddit.listings.More
import dev.gtcl.reddit.listings.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CommentsViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val listingRepository = application.listingRepository

    // Scopes
    private var viewModelJob = Job()
    private var coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    var commentsFetched = false

    private val _post = MutableLiveData<Post>()
    val post: LiveData<Post>
        get() = _post

    private val _comments = MutableLiveData<List<ListingItem>>()
    val comments: LiveData<List<ListingItem>>
        get() = _comments

    fun setPost(redditPost: Post){
        _post.value = redditPost
    }

    fun fetchPostAndComments(permalink: String = post.value!!.permalink){
        coroutineScope.launch {
            val commentPage = listingRepository.getPostAndComments(permalink, CommentSort.BEST).await()
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
            val children = listingRepository.getMoreComments(more.getChildrenAsValidString(), post.value!!.name, CommentSort.BEST).await()
            _moreComments.value = MoreComments(position, more.depth, children.convertChildrenToCommentItems(more.depth))
        }
    }

    fun clearMoreComments(){
        _moreComments.value = null
    }
}