package dev.gtcl.reddit.ui.fragments.reply

import android.os.Parcelable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.repositories.ListingRepository
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReplyVM(private val application: RedditApplication): AndroidViewModel(application) {

    // Repos
    private val listingRepository = ListingRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _newReply = MutableLiveData<NewReply?>()
    val newReply: LiveData<NewReply?>
        get() = _newReply

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    fun newReplyObserved(){
        _newReply.value = null
    }

    fun reply(parent: Item, body: String, position: Int){
        coroutineScope.launch {
            try{
                val newComment = listingRepository.addComment(parent.name, body).await().json.data.things[0].data
                val newPosition = if(parent is Comment){
                    position + 1
                } else{
                    position
                }
                val depth = if(parent is Comment){
                    (parent.depth ?: 0) + 1
                } else {
                    0
                }
                if(newComment is Comment){
                    newComment.depth = depth
                }
                _newReply.value = NewReply(newComment, newPosition)
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            }
        }
    }

    @Parcelize
    data class NewReply(
        val item: Item,
        val position: Int
    ): Parcelable
}