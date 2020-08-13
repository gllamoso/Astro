package dev.gtcl.reddit.ui.fragments.account

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.squareup.moshi.JsonDataException
import dev.gtcl.reddit.NotLoggedInException
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.getErrorMessage
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.models.reddit.listing.Account
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AccountFragmentVM(val application: RedditApplication): ViewModel() {

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

    fun addUser(username: String){
        coroutineScope.launch {
            try {
                val test = userRepository.addFriend(username).await()
                Log.d("TAE", "Add: $test")
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun blockUser(username: String){
        coroutineScope.launch {
            try {
                val response = userRepository.blockUser(username).await()
                if(!response.isSuccessful){
                    throw Exception()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun unblockUser(username: String){
        coroutineScope.launch {
            try {
                val response = userRepository.unblockUser(username).await()
                if(!response.isSuccessful){
                    throw Exception()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

}