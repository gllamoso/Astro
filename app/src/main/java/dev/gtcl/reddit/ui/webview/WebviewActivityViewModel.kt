package dev.gtcl.reddit.ui.webview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WebviewActivityViewModel : ViewModel() {

    private val _redirectUrl = MutableLiveData<String>()
    val redirectUrl: LiveData<String>
        get() = _redirectUrl

    fun setRedirectUrl(url: String){
        _redirectUrl.value = url
    }
}