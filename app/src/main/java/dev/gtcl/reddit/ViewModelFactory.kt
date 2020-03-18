package dev.gtcl.reddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.ui.MainActivityViewModel
import dev.gtcl.reddit.ui.fragments.account.UserFragmentViewModel
import dev.gtcl.reddit.ui.fragments.posts.MainFragmentViewModel
import dev.gtcl.reddit.ui.fragments.posts.comments.CommentsViewModel
import dev.gtcl.reddit.ui.fragments.posts.listing.ListingViewModel
import dev.gtcl.reddit.ui.webview.WebviewActivityViewModel
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val application: RedditApplication): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when{
            modelClass.isAssignableFrom(ListingViewModel::class.java) -> ListingViewModel(application) as T
            modelClass.isAssignableFrom(MainFragmentViewModel::class.java) -> MainFragmentViewModel(application) as T
            modelClass.isAssignableFrom(CommentsViewModel::class.java) -> CommentsViewModel(application) as T
            modelClass.isAssignableFrom(MainActivityViewModel::class.java) -> MainActivityViewModel(application) as T
            modelClass.isAssignableFrom(WebviewActivityViewModel::class.java) -> WebviewActivityViewModel() as T
            modelClass.isAssignableFrom(UserFragmentViewModel::class.java) -> UserFragmentViewModel(application) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}