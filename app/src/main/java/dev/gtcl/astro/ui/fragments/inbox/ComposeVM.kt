package dev.gtcl.astro.ui.fragments.inbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.getErrorMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ComposeVM(private val application: AstroApplication): AstroViewModel(application){

    private val _isLoading = MutableLiveData<Boolean>().apply { value = false }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _messageSent = MutableLiveData<Boolean>().apply { value = false }
    val messageSent: LiveData<Boolean>
        get() = _messageSent

    private val _userDoesNotExist = MutableLiveData<Any?>()
    val userDoesNotExist: LiveData<Any?>
        get() = _userDoesNotExist

    var initialized = false

    fun userDoesNotExistObserved(){
        _userDoesNotExist.value = null
    }

    fun sendMessage(to: String, subject: String, markdown: String){
        coroutineScope.launch {
            try{
                _isLoading.value = true
                if(!userExists(to)){
                    _userDoesNotExist.value = Any()
                    _isLoading.value
                    return@launch
                }
                val result = miscRepository.sendMessage(to, subject, markdown).await()
                if(result.isSuccessful){
                    _messageSent.value = true
                } else {
                   throw Exception()
                }
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun userExists(query: String): Boolean{
        return try {
            userRepository.getAccountInfo(query).await().data
            true
        } catch (e: HttpException){
            false
        }
    }
}