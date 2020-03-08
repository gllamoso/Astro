package dev.gtcl.reddit.ui.fragments.posts

import android.provider.ContactsContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.Listing
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.database.ReadPost
import dev.gtcl.reddit.posts.ListingType
import dev.gtcl.reddit.posts.Post
import dev.gtcl.reddit.posts.SubredditListing
import dev.gtcl.reddit.subs.Subreddit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PostListViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val postRepository = application.postRepository

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val allReadPosts = postRepository.getReadPostsFromDatabase()

    private val _sortSelected = MutableLiveData<PostSort>()
    val sortSelected: LiveData<PostSort>
        get() = _sortSelected

    private val _listingSelected = MutableLiveData<ListingType>()
    val listingSelected: LiveData<ListingType>
        get() = _listingSelected

    private val _timeSelected = MutableLiveData<Time>()
    val timeSelected: LiveData<Time>
        get() = _timeSelected

    private val postListingsOfSubreddit = MutableLiveData<Listing<Post>>()
    val posts = Transformations.switchMap(postListingsOfSubreddit) { it.pagedList }
    val networkState = Transformations.switchMap(postListingsOfSubreddit) { it.networkState }
    val refreshState = Transformations.switchMap(postListingsOfSubreddit) { it.refreshState }

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

    fun fetchPosts(listingType: ListingType, sortBy: PostSort = PostSort.HOT, timePeriod: Time? = null){
        _listingSelected.value = listingType
        _sortSelected.value = sortBy
        _timeSelected.value = timePeriod
        postListingsOfSubreddit.value = postRepository.getPostsOfSubreddit(application.accessToken, listingType, sortBy, timePeriod, 10)
    }

}