package dev.gtcl.reddit.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.SavedAccount
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.models.reddit.AccessToken
import dev.gtcl.reddit.models.reddit.listing.Flair
import dev.gtcl.reddit.models.reddit.listing.MultiReddit
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.models.reddit.listing.Subreddit
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.reddit.ListingRepository
import dev.gtcl.reddit.repositories.reddit.MiscRepository
import dev.gtcl.reddit.repositories.reddit.SubredditRepository
import dev.gtcl.reddit.repositories.reddit.UserRepository
import dev.gtcl.reddit.ui.fragments.ViewPagerPage
import kotlinx.coroutines.*

class MainActivityVM(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)
    private val subredditRepository = SubredditRepository.getInstance(application)
    private val miscRepository = MiscRepository.getInstance(application)

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

    private val _openChromeTab = MutableLiveData<String?>()
    val openChromeTab: LiveData<String?>
        get() = _openChromeTab

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
                        for(sub: Subreddit in subs){
                            if(favSubs.contains(sub.displayName)){
                                sub.isFavorite = true
                            }
                        }
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
                            if (favSubs.contains(sub.displayName)){
                                sub.isFavorite = true
                            }
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
                    _errorMessage.postValue(e.getErrorMessage(application))
                } finally {
                    _refreshState.postValue(NetworkState.LOADED)
                }
            }
        }
    }

    fun unsubscribe(subscription: Subscription){
        coroutineScope.launch {
            try{
                if (subscription.type == SubscriptionType.USER || subscription.type == SubscriptionType.SUBREDDIT) {
                    val response = subredditRepository.subscribe(subscription, false).await()
                    if(response.code() == 200 || response.code() == 404){
                        subredditRepository.deleteSubscription(subscription)
                    } else {
                        throw Exception()
                    }
                } else {
                    val response = subredditRepository.deleteMultiReddit(subscription.url.removePrefix("/")).await()
                    if(response.code() == 200 || response.code() == 404){
                        subredditRepository.deleteSubscription(subscription)
                    } else {
                        throw Exception()
                    }
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
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
            try{
                val response = subredditRepository.subscribe(subreddit, subscribe).await()
                if(response.isSuccessful || (!subscribe && !response.isSuccessful)){
                    if(subscribe){
                        subredditRepository.insertSubreddit(subreddit)
                    } else {
                        subredditRepository.deleteSubscription(subreddit)
                    }
                } else {
                   throw Exception()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun vote(thingId: String, vote: Vote){
        coroutineScope.launch {
            try {
                val response = miscRepository.vote(thingId, vote).await()
                if (!response.isSuccessful) {
                    throw Exception()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }

        }
    }

    fun save(thingId: String, save: Boolean){
        coroutineScope.launch {
            try {
                val response = if(save){
                    miscRepository.save(thingId).await()
                } else {
                    miscRepository.unsave(thingId).await()
                }
                if(!response.isSuccessful){
                    throw Exception()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun hide(thingId: String, hide: Boolean){
        coroutineScope.launch {
            try {
                val response = if(hide){
                    miscRepository.hide(thingId).await()
                } else {
                    miscRepository.unhide(thingId).await()
                }
                if(!response.isSuccessful){
                    throw Exception()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun report(thingId: String, rule: String, ruleType: RuleType){
        coroutineScope.launch {
            try {
                val response = when(ruleType){
                    RuleType.RULE -> miscRepository.report(thingId, ruleReason = rule).await()
                    RuleType.SITE_RULE -> miscRepository.report(thingId, siteReason = rule).await()
                    RuleType.OTHER -> miscRepository.report(thingId, otherReason = rule).await()
                }

                if(response.code() != 200){
                    throw Exception()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }

        }
    }

    fun delete(thingId: String){
        coroutineScope.launch {
            try{
                val response = miscRepository.delete(thingId).await()
                if(!response.isSuccessful){
                    throw Exception()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun updatePost(post: Post, nsfw: Boolean, spoiler: Boolean, getNotifications: Boolean, flair: Flair?){
        coroutineScope.launch {
            try{
                val prevNsfw = post.nsfw
                val prevSpoiler = post.spoiler
                val prevGetNotifications = post.sendReplies
                val prevFlairText = post.flairText
                val prevFlairTemplateId = post.linkFlairTemplateId

                post.apply {
                    this.nsfw = nsfw
                    this.spoiler = spoiler
                    this.sendReplies = getNotifications
                    this.flairText = flair?.text
                    this.linkFlairTemplateId = flair?.id
                }

                if(prevNsfw != nsfw){
                    miscRepository.markNsfw(post.name, nsfw).await()
                }

                if(prevSpoiler != spoiler){
                    miscRepository.markSpoiler(post.name, spoiler).await()
                }

                if(prevGetNotifications != getNotifications){
                    miscRepository.sendRepliesToInbox(post.name, getNotifications).await()
                }

                if(prevFlairTemplateId != flair?.id || prevFlairText != flair?.text){
                    miscRepository.setFlair(post.name, flair).await()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun showUi(show: Boolean){
        _showUi.value = show
    }

    fun toggleUi(){
        _showUi.value = !(_showUi.value ?: true)
    }

    fun openChromeTab(url: String){
        _openChromeTab.value = url
    }

    fun chromeTabOpened(){
        _openChromeTab.value = null
    }

    fun removeAccount(account: SavedAccount){
        coroutineScope.launch {
            withContext(Dispatchers.IO){
                userRepository.deleteUserInDatabase(account.name)
            }
        }
    }

    private suspend fun fetchAccessToken(refreshToken: String): AccessToken {
        return userRepository.getNewAccessToken("Basic ${getEncodedAuthString()}", refreshToken).await().apply {
            this.refreshToken = refreshToken
        }
    }

}
