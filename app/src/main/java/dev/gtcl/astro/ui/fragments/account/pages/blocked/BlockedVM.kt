package dev.gtcl.astro.ui.fragments.account.pages.blocked

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.User
import dev.gtcl.astro.network.NetworkState
import kotlinx.coroutines.launch

class BlockedVM(private val application: AstroApplication) : AstroViewModel(application) {

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _blocked = MutableLiveData<List<User>>()
    val blocked: LiveData<List<User>>
        get() = _blocked

    private val _removeAt = MutableLiveData<Int?>()
    val removeAt: LiveData<Int?>
        get() = _removeAt

    fun getBlocked() {
        coroutineScope.launch {
            try {
                _networkState.postValue(NetworkState.LOADING)
                _blocked.postValue(userRepository.getBlocked().await().data.children)
                _networkState.postValue(NetworkState.LOADED)
            } catch (e: Exception) {
                val errorMessage = e.getErrorMessage(application)
                _errorMessage.postValue(errorMessage)
                _networkState.postValue(NetworkState.error(errorMessage))
            }
        }
    }

    fun removeAndUnblockAt(position: Int) {
        if (position < 0 || position >= (_blocked.value ?: return).size) {
            return
        }
        coroutineScope.launch {
            try {
                val username = (_blocked.value ?: return@launch)[position].name
                userRepository.unblockUser(username).await()
                _removeAt.postValue(position)
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun removeAtObserved() {
        _removeAt.value = null
    }
}