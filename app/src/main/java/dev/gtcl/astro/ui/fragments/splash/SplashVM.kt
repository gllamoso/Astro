package dev.gtcl.astro.ui.fragments.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.models.reddit.AccessToken
import kotlinx.coroutines.*

class SplashVM(val application: AstroApplication): AstroViewModel(application) {

    private val _ready = MutableLiveData<Boolean?>()
    val ready: LiveData<Boolean?>
        get() = _ready

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

    private suspend fun fetchAccessToken(refreshToken: String): AccessToken {
        return userRepository.getNewAccessToken("Basic ${getEncodedAuthString()}", refreshToken).await().apply {
            this.refreshToken = refreshToken
        }
    }
}