package dev.gtcl.reddit.ui.fragments.account.user

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.Listing
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.listings.ProfileListing
import dev.gtcl.reddit.network.ListingItem
import dev.gtcl.reddit.users.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class UserFragmentViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = application.userRepository
    private val postRepository = application.postRepository

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _user = MutableLiveData<User>()
    val user: LiveData<User>
        get() = _user

    fun fetchUserInfo(username: String){

    }

    // Overview
    private val overviewListing = MutableLiveData<Listing<ListingItem>>()
    val overviewPosts = Transformations.switchMap(overviewListing) { it.pagedList }
    val overviewNetworkState = Transformations.switchMap(overviewListing) { it.networkState }
    val overviewRefreshState = Transformations.switchMap(overviewListing) { it.refreshState }

    // Posts
//    private val postsListing = MutableLiveData<Listing<ListingItem>>()
//    val postsPosts = Transformations.switchMap(postsListing) { it.pagedList }
//    val postsNetworkState = Transformations.switchMap(postsListing) { it.networkState }
//    val postsRefreshState = Transformations.switchMap(postsListing) { it.refreshState }

    // Comments
    private val commentsListing = MutableLiveData<Listing<ListingItem>>()
    val commentsPosts = Transformations.switchMap(commentsListing) { it.pagedList }
    val commentsNetworkState = Transformations.switchMap(commentsListing) { it.networkState }
    val commentsRefreshState = Transformations.switchMap(commentsListing) { it.refreshState }

    // Saved
    private val savedListing = MutableLiveData<Listing<ListingItem>>()
    val savedPosts = Transformations.switchMap(savedListing) { it.pagedList }
    val savedNetworkState = Transformations.switchMap(savedListing) { it.networkState }
    val savedRefreshState = Transformations.switchMap(savedListing) { it.refreshState }

    // Hidden
    private val hiddenListing = MutableLiveData<Listing<ListingItem>>()
    val hiddenPosts = Transformations.switchMap(hiddenListing) { it.pagedList }
    val hiddenNetworkState = Transformations.switchMap(hiddenListing) { it.networkState }
    val hiddenRefreshState = Transformations.switchMap(hiddenListing) { it.refreshState }

    // Upvoted
    private val upvotedListing = MutableLiveData<Listing<ListingItem>>()
    val upvotedPosts = Transformations.switchMap(upvotedListing) { it.pagedList }
    val upvotedNetworkState = Transformations.switchMap(upvotedListing) { it.networkState }
    val upvotedRefreshState = Transformations.switchMap(upvotedListing) { it.refreshState }

    // Downvoted
    private val downvotedListing = MutableLiveData<Listing<ListingItem>>()
    val downvotedPosts = Transformations.switchMap(downvotedListing) { it.pagedList }
    val downvotedNetworkState = Transformations.switchMap(downvotedListing) { it.networkState }
    val downvotedRefreshState = Transformations.switchMap(downvotedListing) { it.refreshState }

    // Gilded

    // Friends

    // Blocked

    fun fetchCurrentUser(){
        coroutineScope.launch {
            val user = userRepository.getUserInfo().await()
            Log.d("TAE", "User: $user")
            _user.value = user
        }
    }

    fun fetchListings(){
        overviewListing.value = postRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.OVERVIEW), PostSort.BEST, null, 30)
//        postsListing.value = postRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.POSTS), PostSort.BEST, null, 30)
        commentsListing.value = postRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.COMMENTS), PostSort.BEST, null, 30)
        savedListing.value = postRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.SAVED), PostSort.BEST, null, 30)
        hiddenListing.value = postRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.HIDDEN), PostSort.BEST, null, 30)
        upvotedListing.value = postRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.UPVOTED), PostSort.BEST, null, 30)
        downvotedListing.value = postRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.DOWNVOTED), PostSort.BEST, null, 30)
    }

    fun retryOverview() {
        val listing = overviewListing.value
        listing?.retry?.invoke()
    }

    fun retryCommentsListing(){
        val listing = commentsListing.value
        listing?.retry?.invoke()
    }

    fun retrySaved(){
        val listing = savedListing.value
        listing?.retry?.invoke()
    }

    fun retryHidden(){
        val listing = hiddenListing.value
        listing?.retry?.invoke()
    }

    fun retryUpvoted(){
        val listing = upvotedListing.value
        listing?.retry?.invoke()
    }

    fun retryDownvoted(){
        val listing = downvotedListing.value
        listing?.retry?.invoke()
    }

}