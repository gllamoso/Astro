package dev.gtcl.reddit.ui.fragments.create_post.type

import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication

class UploadImageVM(application: RedditApplication): AndroidViewModel(application){

    private val _uri = MutableLiveData<Uri?>()
    val uri: LiveData<Uri?>
        get() = _uri

    fun setUri(uri: Uri?){
        _uri.value = uri
    }
}