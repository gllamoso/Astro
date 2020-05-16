package dev.gtcl.reddit.ui.fragments.home.listing

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ListingViewModel(val application: RedditApplication): AndroidViewModel(application) {

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _sortSelected = MutableLiveData<PostSort>()
    val sortSelected: LiveData<PostSort>
        get() = _sortSelected

    private val _listingSelected = MutableLiveData<ListingType>()
    val listingSelected: LiveData<ListingType>
        get() = _listingSelected

    private val _subredditSelected = MutableLiveData<Subreddit>()
    val subredditSelected: LiveData<Subreddit>
        get() = _subredditSelected

    private val _timeSelected = MutableLiveData<Time>()
    val timeSelected: LiveData<Time>
        get() = _timeSelected

//    fun refresh() = postListingsOfSubreddit.value?.refresh?.invoke()
//
    fun retry() {
//        val listing = postListingsOfSubreddit.value
//        listing?.retry?.invoke()
    }

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _initialListing = MutableLiveData<List<Item>>()
    val initialListing: LiveData<List<Item>>
        get() = _initialListing
    private var after: String? = null

    val pageSize = 15

    fun loadInitial(listingType: ListingType, sortBy: PostSort = PostSort.BEST, timePeriod: Time? = null){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            _listingSelected.value = listingType
            _sortSelected.value = sortBy
            _timeSelected.value = timePeriod
            // TODO: Add Subreddit
            val response = listingRepository.getListing(listingType, sortBy, timePeriod, null, pageSize * 3).await()
            _initialListing.value = response.data.children.map { it.data }
            after = response.data.after
            var sub: Subreddit? = null
            if(listingType is SubredditListing){
                sub = (subredditRepository.searchSubreddits(
                    nsfw = true,
                    includeProfiles = false,
                    limit = 1,
                    query = listingType.sub.displayName
                ).await().data.children[0] as SubredditChild).data
            }
            _subredditSelected.value = sub
            syncSubredditWithDatabase()
            _networkState.value = NetworkState.LOADED
        }
    }

    // Right Side Bar Layout

    fun syncSubreddit(){
        coroutineScope.launch {
            syncSubredditWithDatabase()
        }
    }

    private suspend fun syncSubredditWithDatabase(){
        val sub = subredditSelected.value ?: return
        val dbSubsMatchingName = subredditRepository.getSubscribedSubs(sub.displayName)
        val subInDb = if(dbSubsMatchingName.isNotEmpty()) dbSubsMatchingName[0] else null
        sub.userSubscribed = subInDb != null
        sub.isFavorite = subInDb?.isFavorite ?: false
        _subredditSelected.value = null
        _subredditSelected.value = sub // Update
    }

    fun subscribe(subreddit: Subreddit, subscribeAction: SubscribeAction, favorite: Boolean = false){
        coroutineScope.launch {
            subredditRepository.subscribe(subreddit.displayName, subscribeAction).enqueue(object: Callback<Void>{
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.d("TAE", "Failed") // TODO: Handle
                }

                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    coroutineScope.launch {
                        if(subscribeAction == SubscribeAction.SUBSCRIBE) insertSub(subreddit, favorite)
                        else subredditRepository.removeSubreddit(subreddit)
                    }
                }
            })
        }
    }

    fun addToFavorites(subreddit: Subreddit, favorite: Boolean){
        coroutineScope.launch {
            if(favorite) {
                subscribe(subreddit, SubscribeAction.SUBSCRIBE, favorite)
            } else {
                subredditRepository.addToFavorites(subreddit.displayName, favorite)
            }
        }
    }

    suspend fun insertSub(subreddit: Subreddit, favorite: Boolean){
        val sub: Subreddit = if(subreddit.name == "")
            (subredditRepository.searchSubreddits(
                nsfw = true,
                includeProfiles = false,
                limit = 1,
                query = subreddit.displayName
            ).await().data.children[0] as SubredditChild).data
        else subreddit
        sub.isFavorite = favorite
        subredditRepository.insertSubreddit(sub)
    }

    ///

    // Recycler View

    fun loadInitialFinished(){
        _initialListing.value = null
    }

    private val _refreshState = MutableLiveData<NetworkState>()
    val refreshState: LiveData<NetworkState>
        get() = _refreshState

    fun refresh(){
        coroutineScope.launch {
            _refreshState.value = NetworkState.LOADING
            _networkState.value = NetworkState.LOADING
            val response = listingRepository.getListing(listingSelected.value!!, sortSelected.value!!, timeSelected.value, null, pageSize * 3).await()
            _initialListing.value = response.data.children.map { it.data }
            after = response.data.after
            _refreshState.value = NetworkState.LOADED
            _networkState.value = NetworkState.LOADED
        }
    }

    private val _additionalListing = MutableLiveData<List<Item>>()
    val additionalListing: LiveData<List<Item>>
        get() = _additionalListing

    fun loadAfter(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            val response = listingRepository.getListing(listingSelected.value!!, sortSelected.value!!, timeSelected.value, after, pageSize).await()
            _additionalListing.value = response.data.children.map { it.data }
            after = response.data.after
            _networkState.value = NetworkState.LOADED
        }
    }

    fun loadAfterFinished(){
        _additionalListing.value = null
    }

    ////

    // Post Actions

    fun vote(fullname: String, vote: Vote){
        listingRepository.vote(fullname, vote).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun save(id: String){
        listingRepository.save(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}

        })
    }

    fun unsave(id: String){
        listingRepository.unsave(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun hide(id: String){
        listingRepository.hide(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun unhide(id: String){
        listingRepository.unhide(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

}