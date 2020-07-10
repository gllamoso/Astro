package dev.gtcl.reddit.ui.fragments.reply

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.repositories.ListingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.Exception

class ReplyVM(private val application: RedditApplication): AndroidViewModel(application) {
    // Repos
    private val listingRepository = ListingRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _response = MutableLiveData<Item>()
    val response: LiveData<Item>
        get() = _response

    private lateinit var parentName: String

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    fun addComment(body: String){
        coroutineScope.launch {
            try {
                val response = listingRepository.addComment(parentName, body).await().json.data.things[0].data
                _response.value = response
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            }
        }
    }

    fun setParentId(parentName: String){
        this.parentName = parentName
    }
}