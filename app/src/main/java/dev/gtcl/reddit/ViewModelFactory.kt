package dev.gtcl.reddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.ui.activities.main.MainActivityViewModel
import dev.gtcl.reddit.ui.fragments.account.AccountFragmentViewModel
import dev.gtcl.reddit.ui.fragments.home.HomeViewModel
import dev.gtcl.reddit.ui.fragments.comments.CommentsViewModel
import dev.gtcl.reddit.ui.fragments.home.listing.ListingViewModel
import dev.gtcl.reddit.ui.activities.signin.SignInViewModel
import dev.gtcl.reddit.ui.fragments.ListingScrollerViewModel
import dev.gtcl.reddit.ui.fragments.account.pages.about.UserAboutViewModel
import dev.gtcl.reddit.ui.fragments.media.MediaDialogViewModel
import dev.gtcl.reddit.ui.fragments.media.MediaViewModel
import dev.gtcl.reddit.ui.fragments.subreddits.SubredditSelectorViewModel
import dev.gtcl.reddit.ui.fragments.subreddits.mine.MineViewModel
import dev.gtcl.reddit.ui.fragments.subreddits.search.SearchViewModel
import dev.gtcl.reddit.ui.fragments.subreddits.trending.TrendingListViewModel
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
            modelClass.isAssignableFrom(ListingScrollerViewModel::class.java) -> ListingScrollerViewModel(application) as T
            modelClass.isAssignableFrom(MineViewModel::class.java) -> MineViewModel(application) as T
            modelClass.isAssignableFrom(TrendingListViewModel::class.java) -> TrendingListViewModel(application) as T
            modelClass.isAssignableFrom(SubredditSelectorViewModel::class.java) -> SubredditSelectorViewModel(application) as T
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(application) as T
            modelClass.isAssignableFrom(MediaViewModel::class.java) -> MediaViewModel(application) as T
            modelClass.isAssignableFrom(MediaDialogViewModel::class.java) -> MediaDialogViewModel(application) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}