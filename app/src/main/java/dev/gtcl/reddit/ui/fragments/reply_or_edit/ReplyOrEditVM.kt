package dev.gtcl.reddit.ui.fragments.reply_or_edit

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.getErrorMessage
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.repositories.reddit.MiscRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReplyOrEditVM(private val application: RedditApplication): AndroidViewModel(application) {

    // Repos
    private val miscRepository = MiscRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _newItem = MutableLiveData<Item?>()
    val newItem: LiveData<Item?>
        get() = _newItem

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>().apply { value = false }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    fun newReplyObserved(){
        _newItem.value = null
    }

    fun reply(parent: Item, body: String){
        coroutineScope.launch {
            try{
                _isLoading.value = true
                val newComment = miscRepository.addComment(parent.name, body).await().json.data.things[0].data
                _newItem.value = newComment
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun edit(parent: Item, body: String){
        coroutineScope.launch {
            try{
                _isLoading.value = true
                val editResponse = miscRepository.editText(parent.name, body).await()
                _newItem.value = editResponse.json.data.things[0].data
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun errorMessageObserved(){
        _errorMessage.value = null
    }
}