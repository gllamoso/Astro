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

    private val _refreshState = MutableLiveData<NetworkState>()
    val refreshState: LiveData<NetworkState>
        get() = _refreshState

    private val _items = MutableLiveData<ArrayList<Item>>()
    val items: LiveData<ArrayList<Item>>
        get() = _items

    private val _newItems = MutableLiveData<List<Item>>()
    val newItems: LiveData<List<Item>>
        get() = _newItems

    private val loadedIds = HashSet<String>()

    private val readItemIds = HashSet<String>()
    private var after: String? = null

    private val _listingType = MutableLiveData<ListingType>()
    val listingType: LiveData<ListingType>
        get() = _listingType

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private lateinit var postSort: PostSort
    private var t: Time? = null
    private var pageSize = 15
    var initialPageLoaded = false
    private var lastItemReached = false

    fun setListingInfo(listingType: ListingType, postSort: PostSort, t: Time?, pageSize: Int){
        _listingType.value = listingType
        this.postSort = postSort
        this.t = t
        this.pageSize = pageSize
    }

    fun setListingInfo(listingType: ListingType){
        _listingType.value = listingType
    }

    fun setSortAndTime(postSort: PostSort, t: Time?){
        this.postSort = postSort
        this.t = t
    }

    fun setSort(sort: PostSort){
        postSort = sort
    }

    fun loadInitial(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            val response = listingRepository.getListing(listingType.value!!, postSort, t, null, pageSize * 3).await()
            val items  = response.data.children.map { it.data }
            after = response.data.after
            var sub: Subreddit? = null
            if(listingType.value is SubredditListing){
                sub = (subredditRepository.searchSubreddits(
                    nsfw = true,
                    includeProfiles = false,
                    limit = 1,
                    query = (listingType.value!! as SubredditListing).sub.displayName
                ).await().data.children[0] as SubredditChild).data
            }
            _subredditSelected.value = sub
            syncSubredditWithDatabase()
            _networkState.value = NetworkState.LOADED
        }
    }

    fun loadInitialDataAndFirstPage(){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            loadFirstPage()
            _networkState.value = NetworkState.LOADED
        }
    }

    private suspend fun loadFirstPage(){
        try {
            // Load subreddit Info
            var sub: Subreddit? = null
            if(listingType.value is SubredditListing){
                sub = (subredditRepository.searchSubreddits(
                    nsfw = true,
                    includeProfiles = false,
                    limit = 1,
                    query = (listingType.value!! as SubredditListing).sub.displayName
                ).await().data.children[0] as SubredditChild).data
            }
            _subredditSelected.value = sub
            syncSubredditWithDatabase()

            // Get listing items
            val response = listingRepository.getListing(listingType.value!!, postSort, t, null, pageSize * 3).await()
            val items = ArrayList(response.data.children.map { it.data })
            listingRepository.getReadPosts().map { it.name }.toCollection(readItemIds)
            setItemsReadStatus(items, readItemIds)
            _items.value = items
            lastItemReached = items.size < (pageSize * 3)
            loadedIds.clear()
            loadedIds.addAll(items.map { it.name })
            after = response.data.after
        } catch (e: Exception){
            _errorMessage.value = e.toString()
        }
    }

    fun loadAfter(){
        if(lastItemReached){
            return
        }
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            try{
                val response = listingRepository.getListing(
                        listingType.value!!,
                        postSort,
                        t,
                        after,
                        pageSize
                    ).await()
                val newItems = response.data.children.map { it.data }.filter { !loadedIds.contains(it.name) }

                lastItemReached = newItems.isEmpty()
                if(lastItemReached){
                    _networkState.value = NetworkState.LOADED
                    return@launch
                }

                loadedIds.addAll(newItems.map { it.name })
                setItemsReadStatus(newItems, readItemIds)
                _items.value!!.addAll(newItems)
                _newItems.value = newItems.toList()
                after = response.data.after
                _networkState.value = NetworkState.LOADED
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            }
        }
    }

    fun newItemsAdded(){
        _newItems.value = null
    }

    fun refresh(){
        coroutineScope.launch {
            _refreshState.value = NetworkState.LOADING
            loadFirstPage()
            _refreshState.value = NetworkState.LOADED
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

    companion object{
        fun setItemsReadStatus(items: List<Item>, readIds: HashSet<String>){
            if(readIds.isEmpty()){
                return
            }
            for(item: Item in items){
                if(item is Post){
                    item.isRead = readIds.contains(item.name)
                }
            }
        }
    }

}