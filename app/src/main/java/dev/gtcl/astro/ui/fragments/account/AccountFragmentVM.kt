package dev.gtcl.astro.ui.fragments.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.squareup.moshi.JsonDataException
import dev.gtcl.astro.R
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.listing.Account
import dev.gtcl.astro.repositories.reddit.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountFragmentVM(val application: AstroApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _account = MutableLiveData<Account>()
    val account: LiveData<Account>
        get() = _account

    private var _username: String? = null
    val username: String?
        get() = _username

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun setUsername(username: String?){
        _username = username
    }

    fun errorMessageObserved(){
        _errorMessage.value = null
    }

    fun fetchAccount(user: String?){
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

    fun addFriend(username: String){
        coroutineScope.launch {
            try {
                userRepository.addFriend(username).await()
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun unfriend(username: String){
        coroutineScope.launch {
            try{
                userRepository.removeFriend(username).await()
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun blockUser(username: String){
        coroutineScope.launch {
            try {
                userRepository.blockUser(username).await()
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun unblockUser(username: String){
        coroutineScope.launch {
            try {
                userRepository.unblockUser(username).await()
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

}