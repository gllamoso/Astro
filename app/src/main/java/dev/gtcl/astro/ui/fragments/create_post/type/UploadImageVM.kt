package dev.gtcl.astro.ui.fragments.create_post.type

import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.astro.AstroApplication

class UploadImageVM: ViewModel(){

    private val _uri = MutableLiveData<Uri?>()
    val uri: LiveData<Uri?>
        get() = _uri

    fun setUri(uri: Uri?){
        _uri.value = uri
    }
}