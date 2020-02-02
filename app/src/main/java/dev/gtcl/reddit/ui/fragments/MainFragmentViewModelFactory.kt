package dev.gtcl.reddit.ui.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import java.lang.IllegalArgumentException


class MainFragmentViewModelFactory(private val application: RedditApplication, val refreshAccessToken: suspend () -> (Unit)) : ViewModelProvider.Factory{
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MainFragmentViewModel::class.java))
            return MainFragmentViewModel(application, refreshAccessToken) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}