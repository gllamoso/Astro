package dev.gtcl.astro.ui.fragments.account.pages.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.squareup.moshi.JsonDataException
import dev.gtcl.astro.R
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.listing.Account
import dev.gtcl.astro.models.reddit.listing.TrophyListingResponse
import kotlinx.coroutines.launch

class AccountAboutVM(val application: AstroApplication) : AstroViewModel(application){

    private val _account = MutableLiveData<Account>()
    val account: LiveData<Account>
        get() = _account

    private var username: String? = null

    fun fetchAccount(user: String?){
        username = user
        coroutineScope.launch {
            try {
                if (user != null) {
                    _account.postValue(userRepository.getAccountInfo(user).await().data)
                } else {
                    _account.postValue(userRepository.getCurrentAccountInfo().await())
                }
            } catch (e: Exception){
                _errorMessage.postValue(if(e is JsonDataException){
                        application.getString(R.string.account_error)
                    } else {
                        e.getErrorMessage(application)
                    }
                )
            }
        }
    }

    // Awards
    private val trophyListing = MutableLiveData<TrophyListingResponse>()
    val awards = Transformations.map(trophyListing){ it.data.trophies.map { trophy -> trophy.data } }!!

    fun fetchAwards(){
        coroutineScope.launch {
            try {
                trophyListing.postValue(miscRepository.getAwards(username ?: application.currentAccount!!.name).await())
            } catch (e: Exception){
                _errorMessage.postValue(if(e is JsonDataException){
                        application.getString(R.string.account_error)
                    } else {
                        e.getErrorMessage(application)
                    }
                )
            }
        }
    }
}