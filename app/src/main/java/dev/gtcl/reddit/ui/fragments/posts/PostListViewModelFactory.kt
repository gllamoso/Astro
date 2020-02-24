package dev.gtcl.reddit.ui.fragments.posts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import java.lang.IllegalArgumentException

class PostListViewModelFactory(private val application: RedditApplication): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(PostListViewModel::class.java))
            return PostListViewModel(application) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}