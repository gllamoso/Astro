package dev.gtcl.reddit.ui.fragments

import androidx.lifecycle.*
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditChild
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViewPagerVM(private val application: RedditApplication): AndroidViewModel(application){

    // Repositories
    private val listingRepository = ListingRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    var isViewPagerSwipeEnabled = false
    var pages: ArrayList<PageType>? = null

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

//     _____          _                  _   _
//    |  __ \        | |       /\       | | (_)
//    | |__) |__  ___| |_     /  \   ___| |_ _  ___  _ __  ___
//    |  ___/ _ \/ __| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | (_) \__ \ |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_|   \___/|___/\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    fun vote(fullname: String, vote: Vote){
        listingRepository.vote(fullname, vote).enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                _errorMessage.value = t.message
            }
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun save(id: String){
        listingRepository.save(id).enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                _errorMessage.value = t.message
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun unsave(id: String){
        listingRepository.unsave(id).enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                _errorMessage.value = t.message
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun hide(id: String){
        listingRepository.hide(id).enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                _errorMessage.value = t.message
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

    fun unhide(id: String){
        listingRepository.unhide(id).enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                _errorMessage.value = t.message
            }
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
        })
    }

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    fun subscribe(subreddit: Subreddit, subscribeAction: SubscribeAction, favorite: Boolean){
        coroutineScope.launch {
            subredditRepository.subscribe(subreddit.displayName, subscribeAction).enqueue(object:
                Callback<Void> {
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    _errorMessage.value = t.message
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
        val sub: Subreddit = if(subreddit.name == ""){
            (subredditRepository
                .searchSubreddits(nsfw = true, includeProfiles = false, limit = 1, query = subreddit.displayName).await()
                .data
                .children[0] as SubredditChild)
                .data
        } else {
            subreddit
        }
        sub.isFavorite = favorite
        subredditRepository.insertSubreddit(sub)
    }

    companion object {
        private val TAG = ViewPagerVM::class.qualifiedName
    }
}