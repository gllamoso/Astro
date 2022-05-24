package dev.gtcl.astro.ui.fragments.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonDataException
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.R
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.listing.Account
import kotlinx.coroutines.launch

class AccountFragmentVM(val application: AstroApplication) : AstroViewModel(application) {

    var selectedPage = 0

    private val _account = MutableLiveData<Account>()
    val account: LiveData<Account>
        get() = _account

    private var _username: String? = null
    val username: String?
        get() = _username

    fun setUsername(username: String?) {
        _username = username
    }

    fun fetchAccount(user: String?) {
        coroutineScope.launch {
            try {
                if (user != null) {
                    _account.postValue(userRepository.getAccountInfo(user).await().data)
                } else {
                    _account.postValue(userRepository.getCurrentAccountInfo().await())
                }
            } catch (e: Exception) {
                _errorMessage.postValue(
                    if (e is JsonDataException) {
                        application.getString(R.string.account_error)
                    } else {
                        e.getErrorMessage(application)
                    }
                )
            }
        }
    }

    fun addFriend(username: String) {
        coroutineScope.launch {
            try {
                userRepository.addFriend(username).await()
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun unfriend(username: String) {
        coroutineScope.launch {
            try {
                userRepository.removeFriend(username).await()
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun blockUser(username: String) {
        coroutineScope.launch {
            try {
                userRepository.blockUser(username).await()
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun unblockUser(username: String) {
        coroutineScope.launch {
            try {
                userRepository.unblockUser(username).await()
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

}