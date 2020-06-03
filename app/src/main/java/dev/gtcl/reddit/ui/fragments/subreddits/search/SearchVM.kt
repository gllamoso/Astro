package dev.gtcl.reddit.ui.fragments.subreddits.search

import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubscriptionType
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditChild
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.SubredditRepository
import dev.gtcl.reddit.repositories.UserRepository
import dev.gtcl.reddit.setSubsAndFavorites
import kotlinx.coroutines.*
import retrofit2.HttpException
import java.util.*

class SearchVM(application: RedditApplication) : AndroidViewModel(application){
    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private var favoriteSubsHash: HashSet<String>? = null
    private var subscribedSubsHash: HashSet<String>? = null
    private var favoriteAccountsHash: HashSet<String>? = null
    private var subscribedAccountsHash: HashSet<String>? = null

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _searchItems = MutableLiveData<List<Item>?>()
    val searchItems: LiveData<List<Item>?>
        get() = _searchItems

    fun searchSubreddits(query: String){
        coroutineScope.launch {
            _networkState.value = NetworkState.LOADING
            val results = ArrayList<Item>()
            fetchAccountIfItExists(query, results)
            fetchSubreddits(query, results)
            _searchItems.value = results
            _networkState.value = NetworkState.LOADED
        }
    }

    private suspend fun fetchAccountIfItExists(query: String, results: MutableList<Item>){
        try {
            val account = userRepository.getAccountInfo(query).await().data

            if(favoriteAccountsHash == null){
                favoriteAccountsHash = subredditRepository.getMyFavoriteSubscriptions(SubscriptionType.USER).map { it.displayName }.toHashSet()
            }

            if(subscribedAccountsHash == null){
                subscribedAccountsHash = subredditRepository.getMySubscriptions(SubscriptionType.USER).map { it.displayName }.toHashSet()
            }

            account.isFavorite = favoriteAccountsHash?.contains(account.name) ?: false
            account.isSubscribed = subscribedAccountsHash?.contains(account.name) ?: false
            results.add(account)
        } catch (e: HttpException){
            if(e.code() == 404){
                Log.d("Search", "Account not found: ${e.code()}")
            } else {
                throw e
            }
        }
    }

    private suspend fun fetchSubreddits(query: String, results: MutableList<Item>){
        val subs = subredditRepository.searchSubredditsFromReddit(
            nsfw = true,
            includeProfiles = false,
            limit = 10,
            query = query
        ).await().data.children.map { (it as SubredditChild).data }

        if(favoriteSubsHash == null){
            favoriteSubsHash = subredditRepository.getMyFavoriteSubscriptions(SubscriptionType.SUBREDDIT).map { it.displayName }.toHashSet()
        }

        if(subscribedSubsHash == null){
            subscribedSubsHash = subredditRepository.getMySubscriptions(SubscriptionType.SUBREDDIT).map { it.displayName }.toHashSet()
        }

        setSubsAndFavorites(subs, subscribedSubsHash!!, favoriteSubsHash!!)
        results.addAll(subs)
    }

    fun searchComplete(){
        _searchItems.value = null
    }

    fun addToFavorites(subreddit: Subreddit){
        favoriteSubsHash?.add(subreddit.displayName)
    }

    fun addToFavorites(account: Account){
        favoriteAccountsHash?.add(account.name)
    }

    fun addToSubscription(subreddit: Subreddit){
        subscribedSubsHash?.add(subreddit.displayName)
    }

    fun addToSubscriptions(account: Account){
        subscribedAccountsHash?.add(account.name)
    }

}