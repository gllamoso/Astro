package dev.gtcl.reddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.ui.activities.MainActivityViewModel
import dev.gtcl.reddit.ui.fragments.account.user.UserFragmentViewModel
import dev.gtcl.reddit.ui.fragments.home.HomeViewPagerViewModel
import dev.gtcl.reddit.ui.fragments.comments.CommentsViewModel
import dev.gtcl.reddit.ui.fragments.home.listing.ListingViewModel
import dev.gtcl.reddit.ui.activities.signin.SignInViewModel
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val application: RedditApplication): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when{
            modelClass.isAssignableFrom(ListingViewModel::class.java) -> ListingViewModel(application) as T
            modelClass.isAssignableFrom(HomeViewPagerViewModel::class.java) -> HomeViewPagerViewModel(application) as T
            modelClass.isAssignableFrom(CommentsViewModel::class.java) -> CommentsViewModel(application) as T
            modelClass.isAssignableFrom(MainActivityViewModel::class.java) -> MainActivityViewModel(
                application
            ) as T
            modelClass.isAssignableFrom(SignInViewModel::class.java) -> SignInViewModel() as T
            modelClass.isAssignableFrom(UserFragmentViewModel::class.java) -> UserFragmentViewModel(
                application
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}