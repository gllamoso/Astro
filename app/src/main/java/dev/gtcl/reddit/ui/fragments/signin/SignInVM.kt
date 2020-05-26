package dev.gtcl.reddit.ui.fragments.signin

import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.getEncodedAuthString
import dev.gtcl.reddit.models.reddit.AccessToken
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.*
import java.util.*
import kotlin.IllegalArgumentException

val STATE = UUID.randomUUID().toString()

class SignInVM(private val application: RedditApplication) : AndroidViewModel(application) {

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
            userRepository.insertUserToDatabase(account)

            application.accessToken = token
            application.currentAccount = account
            _successfullyAddedAccount.value = true
            _loading.value = false
        }
    }

    private suspend fun getAccessToken(code: String): AccessToken{
        return userRepository.postCode(
            "Basic ${getEncodedAuthString(application.baseContext)}",
            code,
            application.getString(R.string.redirect_uri)
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

}