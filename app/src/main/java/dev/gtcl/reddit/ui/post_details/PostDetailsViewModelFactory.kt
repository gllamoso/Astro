package dev.gtcl.reddit.ui.post_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import java.lang.IllegalArgumentException

class PostDetailsViewModelFactory(private val application: RedditApplication): ViewModelProvider.Factory{
    @Suppress("unchecked_cast")
    override fun <T: ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(PostDetailsViewModel::class.java)){
            return PostDetailsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}