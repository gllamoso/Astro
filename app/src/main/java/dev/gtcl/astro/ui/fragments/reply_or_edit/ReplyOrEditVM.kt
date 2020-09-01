package dev.gtcl.astro.ui.fragments.reply_or_edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.listing.Item
import kotlinx.coroutines.launch

class ReplyOrEditVM(private val application: AstroApplication): AstroViewModel(application) {

    private val _newItem = MutableLiveData<Item?>()
    val newItem: LiveData<Item?>
        get() = _newItem

    private val _isLoading = MutableLiveData<Boolean>().apply { value = false }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    fun newReplyObserved(){
        _newItem.value = null
    }

    fun reply(parent: Item, body: String){
        coroutineScope.launch {
            try{
                _isLoading.postValue(true)
                val newComment = miscRepository.addComment(parent.name, body).await().json.data.things[0].data
                _newItem.postValue(newComment)
            } catch (e: Exception){
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun edit(parent: Item, body: String){
        coroutineScope.launch {
            try{
                _isLoading.postValue(true)
                val editResponse = miscRepository.editText(parent.name, body).await()
                _newItem.postValue(editResponse.json.data.things[0].data)
            } catch (e: Exception){
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}