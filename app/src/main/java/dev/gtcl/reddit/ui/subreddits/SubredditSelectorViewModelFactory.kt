package dev.gtcl.reddit.ui.subreddits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import java.lang.IllegalArgumentException

class SubredditSelectorViewModelFactory(private val application: RedditApplication): ViewModelProvider.Factory{
    @Suppress("unchecked_cast")
    override fun <T: ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(SubredditSelectorViewModel::class.java)){
            return SubredditSelectorViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}