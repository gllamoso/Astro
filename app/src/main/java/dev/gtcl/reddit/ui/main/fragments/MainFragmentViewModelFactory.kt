package dev.gtcl.reddit.ui.main.fragments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import java.lang.IllegalArgumentException


class MainFragmentViewModelFactory(private val application: RedditApplication) : ViewModelProvider.Factory{
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MainFragmentViewModel::class.java))
            return MainFragmentViewModel(
                application
            ) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}