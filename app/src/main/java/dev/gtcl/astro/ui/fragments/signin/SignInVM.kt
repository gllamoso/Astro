package dev.gtcl.astro.ui.fragments.signin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.AccessToken
import dev.gtcl.astro.models.reddit.listing.Account
import dev.gtcl.astro.repositories.reddit.UserRepository
import kotlinx.coroutines.*
import java.util.*
import kotlin.IllegalArgumentException

val STATE = UUID.randomUUID().toString()

class SignInVM(private val application: AstroApplication) : AndroidViewModel(application) {

    private val userRepository = UserRepository.getInstance(application)

    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading

    private val _pageStackCount = MutableLiveData<Int>().apply { value = 0 }
    val pageStackCount: LiveData<Int>
        get() = _pageStackCount

    private val _successfullyAddedAccount = MutableLiveData<Boolean>()
    val successfullyAddedAccount: LiveData<Boolean>
        get() = _successfullyAddedAccount

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    fun incrementStackCount(){
        val count = _pageStackCount.value ?: 0
        _pageStackCount.value = count + 1
    }

    fun decrementStackCount(){
        val count = _pageStackCount.value ?: 1
        _pageStackCount.value = count - 1
    }

    fun setNewUser(responseUrl: String){
        coroutineScope.launch {
            _loading.value = true
            val code: String
            try {
               code = getCodeFromUrl(responseUrl)
            } catch(e: IllegalArgumentException){
                _errorMessage.value = e.localizedMessage
                _loading.value = false
                return@launch
            }

            val token = getAccessToken(code)
            val account = getAccount(token)
            account.refreshToken = token.refreshToken
            userRepository.insertUserToDatabase(account)

            application.accessToken = token
            application.currentAccount = account
            saveUserToSharedPreferences(account)
            _successfullyAddedAccount.value = true
            _loading.value = false
        }
    }

    private suspend fun getAccessToken(code: String): AccessToken{
        return userRepository.postCode(
            "Basic ${getEncodedAuthString()}",
            code,
            REDDIT_REDIRECT_URL
        ).await()
    }

    private suspend fun getAccount(token: AccessToken): Account {
        return userRepository.getAccount(token).await()
    }

    private fun getCodeFromUrl(url: String): String{
        val uri = Uri.parse(url)
        if(uri.getQueryParameter("error") != null){
            if(uri.getQueryParameter("error") == "access_denied"){
                throw IllegalArgumentException("Access Denied: $url")
            } else {
                throw IllegalArgumentException("Error URL: $url")
            }
        }

        val state = uri.getQueryParameter("state")
        if(state == STATE){
            return uri.getQueryParameter("code") ?: throw IllegalArgumentException("Code parameter not found: $url")
        } else {
            throw IllegalArgumentException("State parameter did not match: $url")
        }
    }

    private fun saveUserToSharedPreferences(account: Account){
        val sharedPrefs = application.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            val json = Gson().toJson(account)
            putString(CURRENT_USER_KEY, json)
            commit()
        }
    }

}