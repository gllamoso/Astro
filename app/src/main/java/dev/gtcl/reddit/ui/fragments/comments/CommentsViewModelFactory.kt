package dev.gtcl.reddit.ui.fragments.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import java.lang.IllegalArgumentException

class CommentsViewModelFactory(private val application: RedditApplication): ViewModelProvider.Factory{
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(CommentsViewModel::class.java))
            return CommentsViewModel(application) as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}