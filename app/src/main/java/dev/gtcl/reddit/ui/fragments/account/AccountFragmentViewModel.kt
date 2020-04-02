package dev.gtcl.reddit.ui.fragments.account

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.Listing
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.listings.*
import dev.gtcl.reddit.listings.users.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class AccountFragmentViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)
    private val listingRepository = ListingRepository.getInstance(application, Executors.newFixedThreadPool(5))

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _account = MutableLiveData<Account>()
    val account: LiveData<Account>
        get() = _account

    fun fetchAccount(user: String?){
        username = user
        coroutineScope.launch {
            if(user != null)
                _account.value = userRepository.getAccountInfo(user).await().data
            else
                _account.value = userRepository.getCurrentAccountInfo().await()
        }
    }

    var username: String? = null

    // Overview
    private val overviewListing = MutableLiveData<Listing<Item>>()
    val overviewPosts = Transformations.switchMap(overviewListing) { it.pagedList }

    // Posts
    private val postsListing = MutableLiveData<Listing<Item>>()
    val postsPosts = Transformations.switchMap(postsListing) { it.pagedList }

    // Comments
    private val commentsListing = MutableLiveData<Listing<Item>>()

    // Saved
    private val savedListing = MutableLiveData<Listing<Item>>()
    val savedPosts = Transformations.switchMap(savedListing) { it.pagedList }

    // Hidden
    private val hiddenListing = MutableLiveData<Listing<Item>>()
    val hiddenPosts = Transformations.switchMap(hiddenListing) { it.pagedList }

    // Upvoted
    private val upvotedListing = MutableLiveData<Listing<Item>>()
    val upvotedPosts = Transformations.switchMap(upvotedListing) { it.pagedList }

    // Downvoted
    private val downvotedListing = MutableLiveData<Listing<Item>>()
    val downvotedPosts = Transformations.switchMap(downvotedListing) { it.pagedList }

    // Gilded

    // Friends

    // Blocked

    fun fetchListings(){
        overviewListing.value = listingRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.OVERVIEW), PostSort.BEST, null, 30)
        postsListing.value = listingRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.SUBMITTED), PostSort.BEST, null, 30)
        commentsListing.value = listingRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.COMMENTS), PostSort.BEST, null, 30)
        savedListing.value = listingRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.SAVED), PostSort.BEST, null, 30)
        hiddenListing.value = listingRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.HIDDEN), PostSort.BEST, null, 30)
        upvotedListing.value = listingRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.UPVOTED), PostSort.BEST, null, 30)
        downvotedListing.value = listingRepository.getPostsFromNetwork(ProfileListing(ProfileInfo.DOWNVOTED), PostSort.BEST, null, 30)
    }

    fun retryOverview() {
        val listing = overviewListing.value
        listing?.retry?.invoke()
    }

    fun retryPosts(){
        val listing = postsListing.value
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