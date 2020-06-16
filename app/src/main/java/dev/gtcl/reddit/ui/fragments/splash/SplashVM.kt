package dev.gtcl.reddit.ui.fragments.splash

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.AccessToken
import dev.gtcl.reddit.models.reddit.listing.Account
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SplashVM(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _ready = MutableLiveData<Boolean?>()
    val ready: LiveData<Boolean?>
        get() = _ready

    fun readyComplete(){
        _ready.value = null
    }

    fun setCurrentUser(account: Account?, saveToPreferences: Boolean){
        coroutineScope.launch {
            if(account == null) {
                application.accessToken = null
                application.currentAccount = null
            } else {
                val accessToken = fetchAccessToken(account.refreshToken!!)
                application.accessToken = accessToken
                application.currentAccount = userRepository.getAccount(accessToken).await()
            }

            if(saveToPreferences){
                saveAccountToPreferences(application.currentAccount)
            }
            _ready.value = true
        }
    }

    private fun saveAccountToPreferences(account: Account?){
        val sharedPrefs = application.getSharedPreferences(application.getString(R.string.preferences_file_key), Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            val json = Gson().toJson(account)
            putString(CURRENT_USER_KEY, json)
            commit()
        }
    }

    private suspend fun fetchAccessToken(refreshToken: String): AccessToken {
        return userRepository.getNewAccessToken("Basic ${getEncodedAuthString(application.baseContext)}", refreshToken).await().apply {
            this.refreshToken = refreshToken
        }
    }
}