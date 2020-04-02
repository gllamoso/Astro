package dev.gtcl.reddit.ui.fragments.home.listing

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.*
import dev.gtcl.reddit.listings.ListingItem
import dev.gtcl.reddit.listings.ListingRepository
import dev.gtcl.reddit.listings.ListingType
import dev.gtcl.reddit.network.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executors

class ListingViewModel(val application: RedditApplication): AndroidViewModel(application) {

    // Repos
    private val postRepository = ListingRepository.getInstance(application, Executors.newFixedThreadPool(5))

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _sortSelected = MutableLiveData<PostSort>()
    val sortSelected: LiveData<PostSort>
        get() = _sortSelected

    private val _listingSelected = MutableLiveData<ListingType>()
    val listingSelected: LiveData<ListingType>
        get() = _listingSelected

    private val _timeSelected = MutableLiveData<Time>()
    val timeSelected: LiveData<Time>
        get() = _timeSelected

//    private val postListingsOfSubreddit = MutableLiveData<Listing<ListingItem>>()
//    val posts = Transformations.switchMap(postListingsOfSubreddit) { it.pagedList }
//    val networkState = Transformations.switchMap(postListingsOfSubreddit) { it.networkState }
//    val refreshState = Transformations.switchMap(postListingsOfSubreddit) { it.refreshState }

//    fun refresh() = postListingsOfSubreddit.value?.refresh?.invoke()
//
    fun retry() {
//        val listing = postListingsOfSubreddit.value
//        listing?.retry?.invoke()
    }
//
//    fun fetchPosts(listingType: ListingType, sortBy: PostSort = PostSort.BEST, timePeriod: Time? = null){
//        _listingSelected.value = listingType
//        _sortSelected.value = sortBy
//        _timeSelected.value = timePeriod
//        postListingsOfSubreddit.value = postRepository.getPostsFromNetwork(listingType, sortBy, timePeriod, 10)
//    }

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _initialListing = MutableLiveData<List<ListingItem>>()
    val initialListing: LiveData<List<ListingItem>>
        get() = _initialListing
    private var after: String? = null

    fun loadInitial(listingType: ListingType, sortBy: PostSort = PostSort.BEST, timePeriod: Time? = null){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            _listingSelected.value = listingType
            _sortSelected.value = sortBy
            _timeSelected.value = timePeriod
            val response = postRepository.getListing(listingType, sortBy, timePeriod, null, 20).await()
            _initialListing.value = response.data.children.map { it.data }
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
            val response = postRepository.getListing(listingSelected.value!!, sortSelected.value!!, timeSelected.value, null, 40).await()
            _initialListing.value = response.data.children.map { it.data }
            after = response.data.after
            _refreshState.value = NetworkState.LOADED
        }
    }

    private val _additionalListing = MutableLiveData<List<ListingItem>>()
    val additionalListing: LiveData<List<ListingItem>>
        get() = _additionalListing

    fun loadAfter(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            val response = postRepository.getListing(listingSelected.value!!, sortSelected.value!!, timeSelected.value, after, 20).await()
            _additionalListing.value = response.data.children.map { it.data }
            after = response.data.after
            _networkState.value = NetworkState.LOADED
        }
    }

    fun loadAfterFinished(){
        _additionalListing.value = null
    }

    fun vote(fullname: String, vote: Vote){
        postRepository.vote(fullname, vote).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun save(id: String){
        postRepository.save(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}

        })
    }

    fun unsave(id: String){
        postRepository.unsave(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun hide(id: String){
        postRepository.hide(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun unhide(id: String){
        postRepository.unhide(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

}