package dev.gtcl.astro.ui.fragments.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.getEncodedAuthString
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.AccessToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SplashVM(val application: AstroApplication) : AstroViewModel(application) {

    private val _ready = MutableLiveData<Boolean?>()
    val ready: LiveData<Boolean?>
        get() = _ready

    fun readyObserved() {
        _ready.value = null
    }

    fun setCurrentUser(account: SavedAccount?) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main) {
                        if (account == null) {
                            application.apply {
                                setAccessToken(null)
                                setCurrentAccount(null)
                            }
                        } else {
                            val accessToken = fetchAccessToken(account.refreshToken!!)
                            application.apply {
                                setAccessToken(accessToken)
                                setCurrentAccount(
                                    userRepository.getAccount(accessToken).await()
                                )
                            }
                        }
                    }
                    _ready.postValue(true)
                } catch (e: Exception) {
                    Timber.tag(this::class.simpleName).d(e.toString())
                    _errorMessage.postValue(e.getErrorMessage(application))
                }
            }
        }
    }

    private suspend fun fetchAccessToken(refreshToken: String): AccessToken {
        return userRepository.getNewAccessToken("Basic ${getEncodedAuthString()}", refreshToken)
            .await().apply {
                this.refreshToken = refreshToken
            }
    }

}