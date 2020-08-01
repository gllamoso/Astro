package dev.gtcl.reddit.ui.fragments.splash

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.SavedAccount
import dev.gtcl.reddit.models.reddit.AccessToken
import dev.gtcl.reddit.models.reddit.listing.Account
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SplashVM(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _ready = MutableLiveData<Boolean?>()
    val ready: LiveData<Boolean?>
        get() = _ready

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun readyComplete(){
        _ready.value = null
    }

    fun setCurrentUser(account: SavedAccount?, saveToPreferences: Boolean){
        coroutineScope.launch {
            withContext(Dispatchers.IO){
                try {
                    if(account == null) {
                        application.accessToken = null
                        application.currentAccount = null
                    } else {
                        val accessToken = fetchAccessToken(account.refreshToken!!)
                        application.accessToken = accessToken
                        application.currentAccount = userRepository.getAccount(accessToken).await()
                    }

                    if(saveToPreferences){
                        saveAccountToPreferences(application, account)
                    }
                    _ready.postValue(true)
                } catch (e: Exception){
                    _errorMessage.postValue(e.getErrorMessage(application))
                }
            }
        }
    }

    fun errorMessageObserved(){
        _errorMessage.value = null
    }

    private suspend fun fetchAccessToken(refreshToken: String): AccessToken {
        return userRepository.getNewAccessToken("Basic ${getEncodedAuthString()}", refreshToken).await().apply {
            this.refreshToken = refreshToken
        }
    }
}