package dev.gtcl.reddit.ui.fragments.subreddits

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubscriptionType
import dev.gtcl.reddit.database.Subscription
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

    private val _subreddits = MutableLiveData<List<Subscription>?>()
    val subreddits: LiveData<List<Subscription>?>
        get() = _subreddits

    private val _multireddits = MutableLiveData<List<Subscription>?>()
    val multireddits: LiveData<List<Subscription>?>
        get() = _multireddits

    private val _users = MutableLiveData<List<Subscription>?>()
    val users: LiveData<List<Subscription>?>
        get() = _users

    private val _favorites = MutableLiveData<List<Subscription>?>()
    val favorites: LiveData<List<Subscription>?>
        get() = _favorites

    private val _editSubscription = MutableLiveData<Subscription?>()
    val editSubscription: LiveData<Subscription?>
        get() = _editSubscription

    fun fetchSubscriptions(){
        coroutineScope.launch {
            _favorites.value = subredditRepository.getMyFavoriteSubscriptions()
            _subreddits.value = subredditRepository.getMySubscriptions(SubscriptionType.SUBREDDIT)
            _multireddits.value = subredditRepository.getMySubscriptions(SubscriptionType.MULTIREDDIT)
            _users.value = subredditRepository.getMySubscriptions(SubscriptionType.USER)
        }
    }

    fun subredditsObserved(){
        _subreddits.value = null
    }

    fun multiredditsObserved(){
        _multireddits.value = null
    }

    fun usersObserved(){
        _users.value = null
    }

    fun favoritesObserved(){
        _favorites.value = null
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