package dev.gtcl.astro.ui.fragments.account.pages.about

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.squareup.moshi.JsonDataException
import dev.gtcl.astro.R
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.listing.Account
import dev.gtcl.astro.models.reddit.listing.TrophyListingResponse
import dev.gtcl.astro.repositories.reddit.MiscRepository
import dev.gtcl.astro.repositories.reddit.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountAboutVM(val application: AstroApplication) : AndroidViewModel(application){
    // Repos
    private val userRepository = UserRepository.getInstance(application)
    private val miscRepository = MiscRepository.getInstance(application)

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
                trophyListing.value = miscRepository.getAwards(username ?: application.currentAccount!!.name).await()
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