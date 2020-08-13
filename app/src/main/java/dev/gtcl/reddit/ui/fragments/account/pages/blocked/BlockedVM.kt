package dev.gtcl.reddit.ui.fragments.account.pages.blocked

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.getErrorMessage
import dev.gtcl.reddit.models.reddit.User
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BlockedVM(private val application: RedditApplication): AndroidViewModel(application) {
    // Repos
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _blocked = MutableLiveData<List<User>>()
    val blocked: LiveData<List<User>>
        get() = _blocked

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val _removeAt = MutableLiveData<Int?>()
    val removeAt: LiveData<Int?>
        get() = _removeAt

    fun getBlocked() {
        coroutineScope.launch {
            try {
                _networkState.value = NetworkState.LOADING
                _blocked.value = userRepository.getBlocked().await().data.children
                _networkState.value = NetworkState.LOADED
            } catch (e: Exception) {
                val errorMessage = e.getErrorMessage(application)
                _errorMessage.value = errorMessage
                _networkState.value = NetworkState.error(errorMessage)
            }
        }
    }

    fun removeAndUnblockAt(position: Int){
        if(position < 0 || position >= _blocked.value!!.size){
            return
        }
        coroutineScope.launch {
            try{
                val username = _blocked.value!![position].name
                val response = userRepository.unblockUser(username).await()
                if(!response.isSuccessful){
                    throw Exception()
                }
                _removeAt.value = position
            }catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            }
        }
    }

    fun removeAtObserved(){
        _removeAt.value = null
    }
}