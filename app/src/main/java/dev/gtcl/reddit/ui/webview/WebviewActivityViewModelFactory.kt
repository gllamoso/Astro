package dev.gtcl.reddit.ui.webview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

class WebviewActivityViewModelFactory : ViewModelProvider.Factory{

    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(WebviewActivityViewModel::class.java)){
            return WebviewActivityViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}