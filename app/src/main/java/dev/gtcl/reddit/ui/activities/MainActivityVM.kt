package dev.gtcl.reddit.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.models.reddit.MultiReddit
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.*

class MainActivityVM(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val allUsers = userRepository.getAllUsers()

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _refreshState = MutableLiveData<NetworkState?>()
    val refreshState: LiveData<NetworkState?>
        get() = _refreshState

    fun refreshObserved(){
        _refreshState.value = null
    }

    fun syncSubscriptionsWithReddit(){
        coroutineScope.launch {
            withContext(Dispatchers.Default){
                try {
                    _refreshState.postValue(NetworkState.LOADING)
                    val favSubs = subredditRepository.getMyFavoriteSubscriptionsExcludingMultireddits().map { it.name }.toHashSet()
                    if(application.accessToken == null) {
                        subredditRepository.deleteAllMySubscriptions()
                        val subs = subredditRepository.getMySubredditsFromReddit(100, null).await().data.children.map { it.data as Subreddit }
                        for(sub: Subreddit in subs)
                            if(favSubs.contains(sub.displayName))
                                sub.isFavorite = true
                        subredditRepository.insertSubreddits(subs)
                    }
                    else {
                        val allSubs = mutableListOf<Subreddit>()
                        var subs = subredditRepository.getMySubredditsFromReddit(100, null).await().data.children.map { it.data as Subreddit }
                        while(subs.isNotEmpty()) {
                            allSubs.addAll(subs)
                            val lastSub = subs.last()
                            subs = subredditRepository.getMySubredditsFromReddit(100, after = lastSub.name).await().data.children.map { it.data as Subreddit }
                        }
                        for(sub: Subreddit in allSubs) {
                            if (favSubs.contains(sub.displayName))
                                sub.isFavorite = true
                        }
                        val favMultireddits = subredditRepository.getMyFavoriteSubscriptions(SubscriptionType.MULTIREDDIT).map { it.name }.toHashSet()
                        val multiReddits = subredditRepository.getMyMultiRedditsFromReddit().await().map { it.data }
                        for(multi: MultiReddit in multiReddits){
                            if(favMultireddits.contains(multi.name)){
                                multi.isFavorite = true
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
                val response = subredditRepository.subscribe(subscription.name, SubscribeAction.UNSUBSCRIBE).await()
                if(response.code() == 200 || response.code() == 404){
                    subredditRepository.deleteSubscription(subscription.name)
                } else {
                    _errorMessage.value = response.message()
                }
            } else {
                val response = subredditRepository.deleteMultiReddit(subscription.url.removePrefix("/")).await()
                if(response.code() == 200 || response.code() == 404){
                    subredditRepository.deleteSubscription(subscription.name)
                } else {
                    _errorMessage.value = response.message()
                }
            }
        }
    }

    fun favorite(subscription: Subscription, favorite: Boolean){
        coroutineScope.launch {
            subredditRepository.updateSubscription(subscription.id, favorite)
        }
    }

}
