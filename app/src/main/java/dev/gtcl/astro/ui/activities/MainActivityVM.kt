package dev.gtcl.astro.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.models.reddit.AccessToken
import dev.gtcl.astro.models.reddit.RuleType
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.ui.fragments.view_pager.ViewPagerPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivityVM(val application: AstroApplication) : AstroViewModel(application) {

    val allUsers = userRepository.getAllUsers()

    private val _newViewPagerPage = MutableLiveData<ViewPagerPage?>()
    val newViewPagerPage: LiveData<ViewPagerPage?>
        get() = _newViewPagerPage

    private val _refreshState = MutableLiveData<NetworkState?>()
    val refreshState: LiveData<NetworkState?>
        get() = _refreshState

    private val _showMediaControls = MutableLiveData<Boolean>()
    val showMediaControls: LiveData<Boolean>
        get() = _showMediaControls

    private val _openChromeTab = MutableLiveData<String?>()
    val openChromeTab: LiveData<String?>
        get() = _openChromeTab

    private val _mediaDialogOpened = MutableLiveData<Boolean>().apply { value = false }
    val mediaDialogOpened: LiveData<Boolean>
        get() = _mediaDialogOpened

    private val _newMulti = MutableLiveData<MultiReddit?>()
    val newMulti: LiveData<MultiReddit?>
        get() = _newMulti

    private val _subredditSelected = MutableLiveData<Subreddit?>()
    val subredditSelected: LiveData<Subreddit?>
        get() = _subredditSelected

    fun refreshObserved() {
        _refreshState.value = null
    }

    fun mediaDialogOpened(opened: Boolean) {
        _mediaDialogOpened.value = opened
    }

    fun newViewPagerPage(page: ViewPagerPage) {
        _newViewPagerPage.value = page
    }

    fun newViewPagerPageObserved() {
        _newViewPagerPage.value = null
    }

    fun refreshAccessToken() {
        coroutineScope.launch {
            try {
                val refreshToken =
                    (application.accessToken ?: return@launch).refreshToken ?: return@launch
                val accessToken = fetchAccessToken(refreshToken)
                withContext(Dispatchers.Main) {
                    application.setAccessToken(accessToken)
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun syncSubscriptionsWithReddit() {
        coroutineScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    _refreshState.postValue(NetworkState.LOADING)
                    val favSubs =
                        subredditRepository.getMyFavoriteSubscriptionsExcludingMultireddits()
                            .map { it.name }.toHashSet()
                    if (application.accessToken == null) {
                        subredditRepository.deleteAllMySubscriptions()
                        val subs = subredditRepository.getMySubreddits(100, null)
                            .await().data.children.map { it.data as Subreddit }
                        for (sub: Subreddit in subs) {
                            if (favSubs.contains(sub.displayName)) {
                                sub.isFavorite = true
                            }
                        }
                        subredditRepository.insertSubreddits(subs)
                    } else {
                        val allSubs = mutableListOf<Subreddit>()
                        var subs = subredditRepository.getMySubreddits(100, null)
                            .await().data.children.map { it.data as Subreddit }
                        while (subs.isNotEmpty()) {
                            allSubs.addAll(subs)
                            val lastSub = subs.last()
                            subs = subredditRepository.getMySubreddits(100, after = lastSub.name)
                                .await().data.children.map { it.data as Subreddit }
                        }
                        for (sub: Subreddit in allSubs) {
                            if (favSubs.contains(sub.displayName)) {
                                sub.isFavorite = true
                            }
                        }
                        val favMultireddits =
                            subredditRepository.getMyFavoriteSubscriptions(SubscriptionType.MULTIREDDIT)
                                .map { it.name }.toHashSet()
                        val multiReddits =
                            subredditRepository.getMyMultiReddits().await().map { it.data }
                        for (multi: MultiReddit in multiReddits) {
                            if (favMultireddits.contains(multi.name)) {
                                multi.setFavorite(true)
                            }
                        }
                        subredditRepository.deleteAllMySubscriptions()

                        subredditRepository.insertSubreddits(allSubs)
                        subredditRepository.insertMultiReddits(multiReddits)
                    }
                } catch (e: Exception) {
                    _errorMessage.postValue(e.getErrorMessage(application))
                } finally {
                    _refreshState.postValue(NetworkState.LOADED)
                }
            }
        }
    }

    fun unsubscribe(subscription: Subscription) {
        coroutineScope.launch {
            try {
                if (subscription.type == SubscriptionType.USER || subscription.type == SubscriptionType.SUBREDDIT) {
                    subredditRepository.subscribe(subscription, false).await()
                    subredditRepository.deleteSubscription(subscription)
                } else {
                    subredditRepository.deleteMultiReddit(subscription.url.removePrefix("/"))
                        .await()
                    subredditRepository.deleteSubscription(subscription)
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun favorite(subscription: Subscription, favorite: Boolean) {
        coroutineScope.launch {
            subredditRepository.addToFavorites(subscription, favorite)
        }
    }

    fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        coroutineScope.launch {
            try {
                subredditRepository.subscribe(subreddit, subscribe).await()
                if (subscribe) {
                    subredditRepository.insertSubreddit(subreddit)
                } else {
                    subredditRepository.deleteSubscription(subreddit)
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun vote(thingId: String, vote: Vote) {
        coroutineScope.launch {
            try {
                miscRepository.vote(thingId, vote).await()
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }

        }
    }

    fun save(thingId: String, save: Boolean) {
        coroutineScope.launch {
            try {
                if (save) {
                    miscRepository.save(thingId).await()
                } else {
                    miscRepository.unsave(thingId).await()
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun hide(thingId: String, hide: Boolean) {
        coroutineScope.launch {
            try {
                if (hide) {
                    miscRepository.hide(thingId).await()
                } else {
                    miscRepository.unhide(thingId).await()
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun report(thingId: String, rule: String, ruleType: RuleType) {
        coroutineScope.launch {
            try {
                when (ruleType) {
                    RuleType.RULE -> miscRepository.report(thingId, ruleReason = rule).await()
                    RuleType.SITE_RULE -> miscRepository.report(thingId, siteReason = rule).await()
                    RuleType.OTHER -> miscRepository.report(thingId, otherReason = rule).await()
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }

        }
    }

    fun delete(thingId: String) {
        coroutineScope.launch {
            try {
                miscRepository.delete(thingId).await()
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun updatePost(
        post: Post,
        nsfw: Boolean,
        spoiler: Boolean,
        getNotifications: Boolean,
        flair: Flair?
    ) {
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

        coroutineScope.launch {
            try {
                if (prevNsfw != nsfw) {
                    miscRepository.markNsfw(post.name, nsfw).await()
                }

                if (prevSpoiler != spoiler) {
                    miscRepository.markSpoiler(post.name, spoiler).await()
                }

                if (prevGetNotifications != getNotifications) {
                    miscRepository.sendRepliesToInbox(post.name, getNotifications).await()
                }

                if (prevFlairTemplateId != flair?.id || prevFlairText != flair?.text) {
                    miscRepository.setFlair(post.name, flair).await()
                }

            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun block(item: Item) {
        coroutineScope.launch {
            try {
                miscRepository.block(item.name).await()
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun markMessage(item: Item, read: Boolean) {
        coroutineScope.launch {
            try {
                miscRepository.markMessage(item, read).await()
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun deleteMessage(message: Message) {
        coroutineScope.launch {
            try {
                miscRepository.deleteMessage(message).await()
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun showMediaControls(show: Boolean) {
        _showMediaControls.value = show
    }

    fun toggleUi() {
        _showMediaControls.value = !(_showMediaControls.value ?: true)
    }

    fun openChromeTab(url: String) {
        _openChromeTab.value = url
    }

    fun chromeTabOpened() {
        _openChromeTab.value = null
    }

    fun removeAccount(account: SavedAccount) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                userRepository.deleteUserInDatabase(account.name)
            }
        }
    }

    fun newMultiReddit(multi: MultiReddit) {
        _newMulti.value = multi
    }

    fun newMultiObserved() {
        _newMulti.value = null
    }

    fun subredditSelected(sub: Subreddit) {
        _subredditSelected.value = sub
    }

    fun subredditObserved() {
        _subredditSelected.value = null
    }

    private suspend fun fetchAccessToken(refreshToken: String): AccessToken {
        return userRepository.getNewAccessToken("Basic ${getEncodedAuthString()}", refreshToken)
            .await().apply {
                this.refreshToken = refreshToken
            }
    }

}
