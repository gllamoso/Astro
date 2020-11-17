package dev.gtcl.astro.ui.fragments.signin

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.REDDIT_REDIRECT_URL
import dev.gtcl.astro.getEncodedAuthString
import dev.gtcl.astro.models.reddit.AccessToken
import dev.gtcl.astro.models.reddit.listing.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

val STATE = UUID.randomUUID().toString()

class SignInVM(private val application: AstroApplication) : AstroViewModel(application) {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading

    private val _pageStackCount = MutableLiveData<Int>().apply { value = 0 }
    val pageStackCount: LiveData<Int>
        get() = _pageStackCount

    private val _successfullyAddedAccount = MutableLiveData<Boolean>()
    val successfullyAddedAccount: LiveData<Boolean>
        get() = _successfullyAddedAccount

    fun incrementStackCount() {
        val count = _pageStackCount.value ?: 0
        _pageStackCount.value = count + 1
    }

    fun decrementStackCount() {
        val count = _pageStackCount.value ?: 1
        _pageStackCount.value = count - 1
    }

    fun setNewUser(responseUrl: String) {
        coroutineScope.launch {
            _loading.postValue(true)
            val code: String
            try {
                code = getCodeFromUrl(responseUrl)
            } catch (e: IllegalArgumentException) {
                _errorMessage.postValue(e.localizedMessage)
                _loading.postValue(false)
                return@launch
            }

            val token = getAccessToken(code)
            val account = getAccount(token)
            account.refreshToken = token.refreshToken
            userRepository.insertUserToDatabase(account)

            withContext(Dispatchers.Main) {
                application.apply {
                    setAccessToken(token)
                    setCurrentAccount(account)
                    saveAccount(account.asDbModel())
                }
            }
            _successfullyAddedAccount.postValue(true)
            _loading.postValue(false)
        }
    }

    private suspend fun getAccessToken(code: String): AccessToken {
        return userRepository.postCode(
            "Basic ${getEncodedAuthString()}",
            code,
            REDDIT_REDIRECT_URL
        ).await()
    }

    private suspend fun getAccount(token: AccessToken): Account {
        return userRepository.getAccount(token).await()
    }

    private fun getCodeFromUrl(url: String): String {
        val uri = Uri.parse(url)
        if (uri.getQueryParameter("error") != null) {
            if (uri.getQueryParameter("error") == "access_denied") {
                throw IllegalArgumentException("Access Denied: $url")
            } else {
                throw IllegalArgumentException("Error URL: $url")
            }
        }

        val state = uri.getQueryParameter("state")
        if (state == STATE) {
            return uri.getQueryParameter("code")
                ?: throw IllegalArgumentException("Code parameter not found: $url")
        } else {
            throw IllegalArgumentException("State parameter did not match: $url")
        }
    }

}