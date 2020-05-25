package dev.gtcl.reddit.ui.activities.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SignInVM : ViewModel() {

    private val _redirectUrl = MutableLiveData<String>()
    val redirectUrl: LiveData<String>
        get() = _redirectUrl

    fun setRedirectUrl(url: String){
        _redirectUrl.value = url
    }
}