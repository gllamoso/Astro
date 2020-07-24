package dev.gtcl.reddit.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.models.reddit.AccessToken
import dev.gtcl.reddit.models.reddit.listing.MultiReddit
import dev.gtcl.reddit.models.reddit.listing.Subreddit
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.SubredditRepository
import dev.gtcl.reddit.repositories.UserRepository
import dev.gtcl.reddit.ui.fragments.ViewPagerPage
import kotlinx.coroutines.*

class MainActivityVM(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)
    private val listingRepository = ListingRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val allUsers = userRepository.getAllUsers()

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _newPage = MutableLiveData<ViewPagerPage?>()
    val newPage: LiveData<ViewPagerPage?>
        get() = _newPage

    private val _refreshState = MutableLiveData<NetworkState?>()
    val refreshState: LiveData<NetworkState?>
        get() = _refreshState

    private val _showUi = MutableLiveData<Boolean>()
    val showUi: LiveData<Boolean>
        get() = _showUi

    fun refreshObserved(){
        _refreshState.value = null
    }

    fun newPage(page: ViewPagerPage){
        _newPage.value = page
    }

    fun newPageObserved(){
        _newPage.value = null
    }

    fun refreshAccessToken(){
        coroutineScope.launch {
            val refreshToken = application.accessToken!!.refreshToken!!
            application.accessToken = fetchAccessToken(refreshToken)
        }
    }

    fun syncSubscriptionsWithReddit(){
        coroutineScope.launch {
            withContext(Dispatchers.Default){
                try {
                    _refreshState.postValue(NetworkState.LOADING)
                    val favSubs = subredditRepository.getMyFavoriteSubscriptionsExcludingMultireddits().map { it.name }.toHashSet()
                    if(application.accessToken == null) {
                        subredditRepository.deleteAllMySubscriptions()
                        val subs = subredditRepository.getMySubreddits(100, null).await().data.children.map { it.data as Subreddit }
                        for(sub: Subreddit in subs)
                            if(favSubs.contains(sub.displayName))
                                sub.setFavorite(true)
                        subredditRepository.insertSubreddits(subs)
                    }
                    else {
                        val allSubs = mutableListOf<Subreddit>()
                        var subs = subredditRepository.getMySubreddits(100, null).await().data.children.map { it.data as Subreddit }
                        while(subs.isNotEmpty()) {
                            allSubs.addAll(subs)
                            val lastSub = subs.last()
                            subs = subredditRepository.getMySubreddits(100, after = lastSub.name).await().data.children.map { it.data as Subreddit }
                        }
                        for(sub: Subreddit in allSubs) {
                            if (favSubs.contains(sub.displayName))
                                sub.setFavorite(true)
                        }
                        val favMultireddits = subredditRepository.getMyFavoriteSubscriptions(SubscriptionType.MULTIREDDIT).map { it.name }.toHashSet()
                        val multiReddits = subredditRepository.getMyMultiReddits().await().map { it.data }
                        for(multi: MultiReddit in multiReddits){
                            if(favMultireddits.contains(multi.name)){
                                multi.setFavorite(true)
                            }
                        }
                        subredditRepository.deleteAllMySubscriptions()

                        subredditRepository.insertSubreddits(allSubs)
                        subredditRepository.insertMultiReddits(multiReddits)
                    }
                } catch(e: Exception) {
                    _errorMessage.postValue(e.toString())
                } finally {
                    _refreshState.postValue(NetworkState.LOADED)
                }
            }
        }
    }

    fun unsubscribe(subscription: Subscription){
        coroutineScope.launch {
            if (subscription.type == SubscriptionType.USER || subscription.type == SubscriptionType.SUBREDDIT) {
                val response = subredditRepository.subscribe(subscription, false).await()
                if(response.code() == 200 || response.code() == 404){
                    subredditRepository.deleteSubscription(subscription)
                } else {
                    _errorMessage.value = response.message()
                }
            } else {
                val response = subredditRepository.deleteMultiReddit(subscription.url.removePrefix("/")).await()
                if(response.code() == 200 || response.code() == 404){
                    subredditRepository.deleteSubscription(subscription)
                } else {
                    _errorMessage.value = response.message()
                }
            }
        }
    }

    fun favorite(subscription: Subscription, favorite: Boolean){
        coroutineScope.launch {
            subredditRepository.addToFavorites(subscription, favorite)
        }
    }

    fun subscribe(subreddit: Subreddit, subscribe: Boolean){
        coroutineScope.launch {
            val response = subredditRepository.subscribe(subreddit, subscribe).await()
            if(response.code() == 200 || (!subscribe && response.code() == 404)){
                if(subscribe){
                    subredditRepository.insertSubreddit(subreddit)
                } else {
                    subredditRepository.deleteSubscription(subreddit)
                }
            } else {
                _errorMessage.value = response.message()
            }
        }
    }

    fun vote(thingId: String, vote: Vote){
        coroutineScope.launch {
            val response = listingRepository.vote(thingId, vote).await()
            if (response.code() != 200) {
                _errorMessage.value = response.message()
            }
        }
    }

    fun save(thingId: String, save: Boolean){
        coroutineScope.launch {
            val response = if(save){
                listingRepository.save(thingId).await()
            } else {
                listingRepository.unsave(thingId).await()
            }
            if(response.code() != 200){
                _errorMessage.value = response.message()
            }
        }
    }

    fun hide(thingId: String, hide: Boolean){
        coroutineScope.launch {
            val response = if(hide){
                listingRepository.hide(thingId).await()
            } else {
                listingRepository.unhide(thingId).await()
            }
            if(response.code() != 200){
                _errorMessage.value = response.message()
            }
        }
    }

    fun report(){
        TODO("Implement Reporting")
    }

    fun showUi(show: Boolean){
        _showUi.value = show
    }

    fun toggleUi(){
        _showUi.value = !(_showUi.value ?: true)
    }

    private suspend fun fetchAccessToken(refreshToken: String): AccessToken {
        return userRepository.getNewAccessToken("Basic ${getEncodedAuthString(application.baseContext)}", refreshToken).await().apply {
            this.refreshToken = refreshToken
        }
    }

}
