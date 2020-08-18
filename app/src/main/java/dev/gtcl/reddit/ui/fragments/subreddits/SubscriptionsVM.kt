package dev.gtcl.reddit.ui.fragments.subreddits

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubscriptionType
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.models.reddit.listing.MultiReddit
import dev.gtcl.reddit.models.reddit.listing.MultiRedditUpdate
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.android.synthetic.main.item_post.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SubscriptionsVM(private val application: RedditApplication): AndroidViewModel(application){
    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

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
            _subscriptions.value = Subscriptions(favorites, multiReddits, subreddits, users)
        }
    }

    fun subscriptionsObserved(){
        _subscriptions.value = null
    }

    fun errorMessageObserved(){
        _errorMessage.value = null
    }

    fun createMulti(model: MultiRedditUpdate){
        coroutineScope.launch {
            try{
                val multiReddit = subredditRepository.createMulti(model).await().data
                subredditRepository.insertMultiReddit(multiReddit)
                _editSubscription.value = multiReddit.asSubscription()
            } catch (e: Exception){
                if(e is HttpException && e.code() == 409){
                    _errorMessage.value = application.getString(R.string.conflict_error)
                } else{
                    _errorMessage.value = e.toString()
                }
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