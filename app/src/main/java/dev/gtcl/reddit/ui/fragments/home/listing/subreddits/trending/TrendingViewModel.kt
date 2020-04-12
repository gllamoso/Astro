package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.trending

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubredditWhere
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.listings.ListingRepository
import dev.gtcl.reddit.listings.ListingType
import dev.gtcl.reddit.network.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class TrendingViewModel(application: RedditApplication): AndroidViewModel(application){

    // Repos
    private val listingRepository = ListingRepository.getInstance(application, Executors.newFixedThreadPool(5))

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _initialListing = MutableLiveData<List<TrendingSubredditPost>>()
    val initialListing: LiveData<List<TrendingSubredditPost>>
        get() = _initialListing
    private var after: String? = null
    private var user: String? = null

    private lateinit var listingType: ListingType
    private lateinit var postSort: PostSort
    private var t: Time? = null
    private var pageSize = 40

    fun retry(){
        TODO()
    }

    fun setListingInfo(listingType: ListingType, postSort: PostSort, t: Time?, pageSize: Int){
        this.listingType = listingType
        this.postSort = postSort
        this.t = t
        this.pageSize = pageSize
    }

    private lateinit var subredditWhere: SubredditWhere

    fun loadInitial(){
        coroutineScope.launch {
            val subscribedSubs = listingRepository.getSubscribedSubs()
            _networkState.value = NetworkState.LOADING
            val response = if(::listingType.isInitialized)
                listingRepository.getListing(listingType, postSort, t, null, pageSize, user).await()
            else
                listingRepository.getNetworkSubreddits(subredditWhere, limit = pageSize).await()
            _initialListing.value = response.data.children.map { it.data }.toTrendingPosts()
            after = response.data.after
            _networkState.value = NetworkState.LOADED
        }
    }

    fun loadInitialFinished(){
        _initialListing.value = null
    }

    private val _refreshState = MutableLiveData<NetworkState>()
    val refreshState: LiveData<NetworkState>
        get() = _refreshState

    fun refresh(){
        coroutineScope.launch {
            _refreshState.value = NetworkState.LOADING
            val response = if(::listingType.isInitialized)
                listingRepository.getListing(listingType, postSort, t, null, pageSize, user).await()
            else
                listingRepository.getNetworkSubreddits(subredditWhere, null, pageSize).await()
            _initialListing.value = response.data.children.map { it.data }.toTrendingPosts()
            after = response.data.after
            _refreshState.value = NetworkState.LOADED
        }
    }

    private val _additionalListing = MutableLiveData<List<TrendingSubredditPost>>()
    val additionalListing: LiveData<List<TrendingSubredditPost>>
        get() = _additionalListing

    fun loadAfter(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            val response = if(::listingType.isInitialized)
                listingRepository.getListing(listingType, postSort, t, after, pageSize, user).await()
            else
                listingRepository.getNetworkSubreddits(subredditWhere, after, pageSize).await()
            _additionalListing.value = response.data.children.map { it.data }.toTrendingPosts()
            after = response.data.after
            _networkState.value = NetworkState.LOADED
        }
    }

    fun loadAfterFinished(){
        _additionalListing.value = null
    }

    val subscribedSubs = listingRepository.getSubscribedSubsLive()
}