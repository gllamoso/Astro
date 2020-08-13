package dev.gtcl.reddit.ui.fragments.account.pages.about

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.squareup.moshi.JsonDataException
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.getErrorMessage
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.models.reddit.listing.Account
import dev.gtcl.reddit.models.reddit.listing.TrophyListingResponse
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class UserAboutVM(val application: RedditApplication) : AndroidViewModel(application){
    // Repos
    private val userRepository = UserRepository.getInstance(application)
    private val listingRepository = ListingRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _account = MutableLiveData<Account>()
    val account: LiveData<Account>
        get() = _account

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private var username: String? = null

    fun errorMessageObserved(){
        _errorMessage.value = null
    }

    fun fetchAccount(user: String?){
        username = user
        coroutineScope.launch {
            try {
                if (user != null) {
                    _account.value = userRepository.getAccountInfo(user).await().data
                } else {
                    _account.value = userRepository.getCurrentAccountInfo().await()
                }
            } catch (e: Exception){
                _errorMessage.value = if(e is JsonDataException){
                    application.getString(R.string.account_error)
                } else {
                    e.getErrorMessage(application)
                }
            }
        }
    }

    // Awards
    private val trophyListing = MutableLiveData<TrophyListingResponse>()
    val awards = Transformations.map(trophyListing){ it.data.trophies.map { trophy -> trophy.data } }!!

    fun fetchAwards(){
        coroutineScope.launch {
            try {
                trophyListing.value = listingRepository.getAwards(username ?: application.currentAccount!!.name).await()
            } catch (e: Exception){
                _errorMessage.value = if(e is JsonDataException){
                    application.getString(R.string.account_error)
                } else {
                    e.getErrorMessage(application)
                }
            }
        }
    }
}