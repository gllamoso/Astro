package dev.gtcl.reddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.ui.activities.MainActivityViewModel
import dev.gtcl.reddit.ui.fragments.account.AccountFragmentViewModel
import dev.gtcl.reddit.ui.fragments.home.HomeViewModel
import dev.gtcl.reddit.ui.fragments.comments.CommentsViewModel
import dev.gtcl.reddit.ui.fragments.home.listing.ListingViewModel
import dev.gtcl.reddit.ui.activities.signin.SignInViewModel
import dev.gtcl.reddit.ui.fragments.LoadMoreScrollViewModel
import dev.gtcl.reddit.ui.fragments.account.pages.UserAboutViewModel
import dev.gtcl.reddit.ui.fragments.dialog.subreddits.SubredditSelectorViewModel
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.mine.MineViewModel
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val application: RedditApplication): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when{
            modelClass.isAssignableFrom(ListingViewModel::class.java) -> ListingViewModel(application) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(application) as T
            modelClass.isAssignableFrom(CommentsViewModel::class.java) -> CommentsViewModel(application) as T
            modelClass.isAssignableFrom(MainActivityViewModel::class.java) -> MainActivityViewModel(application) as T
            modelClass.isAssignableFrom(SignInViewModel::class.java) -> SignInViewModel() as T
            modelClass.isAssignableFrom(AccountFragmentViewModel::class.java) -> AccountFragmentViewModel(application) as T
            modelClass.isAssignableFrom(UserAboutViewModel::class.java) -> UserAboutViewModel(application) as T
            modelClass.isAssignableFrom(LoadMoreScrollViewModel::class.java) -> LoadMoreScrollViewModel(application) as T
            modelClass.isAssignableFrom(MineViewModel::class.java) -> MineViewModel(application) as T
            modelClass.isAssignableFrom(SubredditSelectorViewModel::class.java) -> SubredditSelectorViewModel(application) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}