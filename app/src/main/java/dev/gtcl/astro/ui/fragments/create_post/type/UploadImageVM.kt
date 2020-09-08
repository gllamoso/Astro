package dev.gtcl.astro.ui.fragments.create_post.type

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel

class UploadImageVM(application: AstroApplication) : AstroViewModel(application) {

    private val _uri = MutableLiveData<Uri?>()
    val uri: LiveData<Uri?>
        get() = _uri

    fun setUri(uri: Uri?) {
        _uri.value = uri
    }
}