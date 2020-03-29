package dev.gtcl.reddit.ui.fragments.posts.listing

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.ReadListing
import dev.gtcl.reddit.network.ListingItem
import dev.gtcl.reddit.listings.ListingType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ListingViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val postRepository = application.postRepository

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

    private val postListingsOfSubreddit = MutableLiveData<Listing<ListingItem>>()
    val posts = Transformations.switchMap(postListingsOfSubreddit) { it.pagedList }
    val networkState = Transformations.switchMap(postListingsOfSubreddit) { it.networkState }
    val refreshState = Transformations.switchMap(postListingsOfSubreddit) { it.refreshState }

    fun refresh() = postListingsOfSubreddit.value?.refresh?.invoke()

    fun retry() {
        val listing = postListingsOfSubreddit.value
        listing?.retry?.invoke()
    }

    fun fetchPosts(listingType: ListingType, sortBy: PostSort = PostSort.BEST, timePeriod: Time? = null){
        _listingSelected.value = listingType
        _sortSelected.value = sortBy
        _timeSelected.value = timePeriod
        postListingsOfSubreddit.value = postRepository.getPostsFromNetwork(listingType, sortBy, timePeriod, 10)
    }

    fun vote(fullname: String, vote: Vote){
        postRepository.vote(fullname, vote).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("TAE", "Success!") // TODO: Handle
            }
        })
    }

    fun save(id: String){
        postRepository.save(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("TAE", "Success!") // TODO: Handle
            }

        })
    }

    fun unsave(id: String){
        postRepository.unsave(id).enqueue(object: Callback<Void>{
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("TAE", "Failed") // TODO: Handle
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("TAE", "Success!") // TODO: Handle
            }

        })
    }

}