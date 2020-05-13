package dev.gtcl.reddit.ui.fragments

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.MessageRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class LoadMoreScrollViewModel(application: RedditApplication): AndroidViewModel(application){

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)
    private val messageRepository = MessageRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _initialListing = MutableLiveData<List<Item>>()
    val initialListing: LiveData<List<Item>>
        get() = _initialListing
    private var after: String? = null
    private var user: String? = null

    private lateinit var postSort: PostSort
    private var t: Time? = null
    private var pageSize = 15

    val subscribedSubs = subredditRepository.getSubscribedSubsLive()

    fun retry(){
        TODO()
    }

    private lateinit var listingType: ListingType
    fun setListingInfo(listingType: ListingType, postSort: PostSort, t: Time?, pageSize: Int){
        this.listingType = listingType
        this.postSort = postSort
        this.t = t
        this.pageSize = pageSize
    }

    private lateinit var subredditWhere: SubredditWhere
    fun setListingInfo(subredditWhere: SubredditWhere, pageSize: Int){
        this.subredditWhere = subredditWhere
        this.pageSize = pageSize
    }

    private lateinit var messageWhere: MessageWhere
    fun setListingInfo(messageWhere: MessageWhere, pageSize: Int){
        this.messageWhere = messageWhere
        this.pageSize = pageSize
    }

    fun setUser(user: String?){
        this.user = user
    }

    fun loadInitial(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            loadFirstPage()
            _networkState.value = NetworkState.LOADED
        }
    }

    fun loadInitialFinished(){
        _initialListing.value = null
    }

    private val _refreshState = MutableLiveData<NetworkState>()
    val refreshState: LiveData<NetworkState>
        get() = _refreshState

    private suspend fun loadFirstPage(){
        val response = when{
            ::listingType.isInitialized -> listingRepository.getListing(listingType, postSort, t, null, pageSize * 3, user).await()
            ::subredditWhere.isInitialized -> subredditRepository.getNetworkSubreddits(subredditWhere, null, pageSize * 3).await()
            ::messageWhere.isInitialized -> messageRepository.getMessages(messageWhere, null, pageSize * 3).await()
            else -> throw IllegalStateException("Not enough info to load listing")
        }
        _initialListing.value = response.data.children.map { it.data }
        after = response.data.after
    }

    fun refresh(){
        coroutineScope.launch {
            _refreshState.value = NetworkState.LOADING
            loadFirstPage()
            _refreshState.value = NetworkState.LOADED
        }
    }

    private val _additionalListing = MutableLiveData<List<Item>>()
    val additionalListing: LiveData<List<Item>>
        get() = _additionalListing

    fun loadAfter(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            val response = when{
                ::listingType.isInitialized -> listingRepository.getListing(listingType, postSort, t, after, pageSize * 3, user).await()
                ::subredditWhere.isInitialized -> subredditRepository.getNetworkSubreddits(subredditWhere, after, pageSize * 3).await()
                ::messageWhere.isInitialized -> messageRepository.getMessages(messageWhere, after, pageSize * 3).await()
                else -> throw IllegalStateException("Not enough info to load listing")
            }
            _additionalListing.value = response.data.children.map { it.data }
            after = response.data.after
            _networkState.value = NetworkState.LOADED
        }
    }

    fun loadAfterFinished(){
        _additionalListing.value = null
    }

}