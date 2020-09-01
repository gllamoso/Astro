package dev.gtcl.astro.ui.fragments.subscriptions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.models.reddit.listing.MultiRedditUpdate
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SubscriptionsVM(private val application: AstroApplication): AstroViewModel(application){

    private val _subscriptions = MutableLiveData<Subscriptions?>()
    val subscriptions: LiveData<Subscriptions?>
        get() = _subscriptions

    private val _editSubscription = MutableLiveData<Subscription?>()
    val editSubscription: LiveData<Subscription?>
        get() = _editSubscription

    fun fetchSubscriptions(){
        coroutineScope.launch {
            val favorites = subredditRepository.getMyFavoriteSubscriptions()
            val multiReddits = subredditRepository.getMySubscriptions(SubscriptionType.MULTIREDDIT)
            val subreddits = subredditRepository.getMySubscriptions(SubscriptionType.SUBREDDIT)
            val users = subredditRepository.getMySubscriptions(SubscriptionType.USER)
            _subscriptions.postValue(Subscriptions(favorites, multiReddits, subreddits, users))
        }
    }

    fun subscriptionsObserved(){
        _subscriptions.value = null
    }

    fun createMulti(model: MultiRedditUpdate){
        coroutineScope.launch {
            try{
                val multiReddit = subredditRepository.createMulti(model).await().data
                subredditRepository.insertMultiReddit(multiReddit)
                _editSubscription.postValue(multiReddit.asSubscription())
            } catch (e: Exception){
                _errorMessage.postValue(
                    if(e is HttpException && e.code() == 409){
                        application.getString(R.string.conflict_error)
                    } else{
                        e.getErrorMessage(application)
                    }
                )

            }
        }
    }

}

data class Subscriptions(
    val favorites: List<Subscription>,
    val multiReddits: List<Subscription>,
    val subreddits: List<Subscription>,
    val users: List<Subscription>
)