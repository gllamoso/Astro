package dev.gtcl.astro.ui.fragments.subscriptions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.SubscriptionType
import dev.gtcl.astro.database.Subscription
import kotlinx.coroutines.launch

class SubscriptionsVM(application: AstroApplication) : AstroViewModel(application) {

    private val _subscriptions = MutableLiveData<Subscriptions?>()
    val subscriptions: LiveData<Subscriptions?>
        get() = _subscriptions

    fun fetchSubscriptions() {
        coroutineScope.launch {
            val favorites = subredditRepository.getMyFavoriteSubscriptions()
            val multiReddits = subredditRepository.getMySubscriptions(SubscriptionType.MULTIREDDIT)
            val subreddits = subredditRepository.getMySubscriptions(SubscriptionType.SUBREDDIT)
            val users = subredditRepository.getMySubscriptions(SubscriptionType.USER)
            _subscriptions.postValue(Subscriptions(favorites, multiReddits, subreddits, users))
        }
    }

    fun subscriptionsObserved() {
        _subscriptions.value = null
    }

}

data class Subscriptions(
    val favorites: List<Subscription>,
    val multiReddits: List<Subscription>,
    val subreddits: List<Subscription>,
    val users: List<Subscription>
)