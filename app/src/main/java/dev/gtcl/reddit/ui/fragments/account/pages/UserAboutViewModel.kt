package dev.gtcl.reddit.ui.fragments.account.pages

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.models.reddit.TrophyListingResponse
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class UserAboutViewModel(val application: RedditApplication) : AndroidViewModel(application){
    // Repos
    private val userRepository = UserRepository.getInstance(application)
    private val listingRepository = ListingRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _account = MutableLiveData<Account>()
    val account: LiveData<Account>
        get() = _account

    private var username: String? = null

    fun fetchAccount(user: String?){
        username = user
        coroutineScope.launch {
            if(user != null)
                _account.value = userRepository.getAccountInfo(user).await().data
            else
                _account.value = userRepository.getCurrentAccountInfo().await()
        }
    }

    // Awards
    private val trophyListing = MutableLiveData<TrophyListingResponse>()
    val awards = Transformations.map(trophyListing){ it.data.trophies.map { trophy -> trophy.data } }

    fun fetchAwards(){
        coroutineScope.launch {
            trophyListing.value = listingRepository.getAwards(username ?: application.currentAccount!!.name).await()
        }
    }
}