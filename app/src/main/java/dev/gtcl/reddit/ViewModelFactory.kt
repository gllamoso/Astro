package dev.gtcl.reddit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.account.AccountFragmentVM
import dev.gtcl.reddit.ui.fragments.ViewPagerVM
import dev.gtcl.reddit.ui.fragments.comments.CommentsVM
import dev.gtcl.reddit.ui.fragments.listing.ListingVM
import dev.gtcl.reddit.ui.fragments.item_scroller.ItemScrollerVM
import dev.gtcl.reddit.ui.fragments.account.pages.about.UserAboutVM
import dev.gtcl.reddit.ui.fragments.create_post.CreatePostVM
import dev.gtcl.reddit.ui.fragments.create_post.type.UploadImageVM
import dev.gtcl.reddit.ui.fragments.media.MediaDialogVM
import dev.gtcl.reddit.ui.fragments.media.list.item.MediaVM
import dev.gtcl.reddit.ui.fragments.reply.ReplyVM
import dev.gtcl.reddit.ui.fragments.signin.SignInVM
import dev.gtcl.reddit.ui.fragments.splash.SplashVM
import dev.gtcl.reddit.ui.fragments.subreddits.SubscriptionsVM
import dev.gtcl.reddit.ui.fragments.subreddits.multireddit.MultiRedditVM
import dev.gtcl.reddit.ui.fragments.subreddits.search.SearchVM
import dev.gtcl.reddit.ui.fragments.subreddits.trending.TrendingListVM
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val application: RedditApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SplashVM::class.java) -> SplashVM(application) as T
            modelClass.isAssignableFrom(SignInVM::class.java) -> SignInVM(application) as T
            modelClass.isAssignableFrom(ListingVM::class.java) -> ListingVM(application) as T
            modelClass.isAssignableFrom(ViewPagerVM::class.java) -> ViewPagerVM(application) as T
            modelClass.isAssignableFrom(CommentsVM::class.java) -> CommentsVM(application) as T
            modelClass.isAssignableFrom(MainActivityVM::class.java) -> MainActivityVM(application) as T
            modelClass.isAssignableFrom(AccountFragmentVM::class.java) -> AccountFragmentVM(application) as T
            modelClass.isAssignableFrom(UserAboutVM::class.java) -> UserAboutVM(application) as T
            modelClass.isAssignableFrom(ItemScrollerVM::class.java) -> ItemScrollerVM(application) as T
            modelClass.isAssignableFrom(TrendingListVM::class.java) -> TrendingListVM(application) as T
            modelClass.isAssignableFrom(SubscriptionsVM::class.java) -> SubscriptionsVM(application) as T
            modelClass.isAssignableFrom(SearchVM::class.java) -> SearchVM(application) as T
            modelClass.isAssignableFrom(MultiRedditVM::class.java) -> MultiRedditVM(application) as T
            modelClass.isAssignableFrom(ReplyVM::class.java) -> ReplyVM(application) as T
            modelClass.isAssignableFrom(CreatePostVM::class.java) -> CreatePostVM(application) as T
            modelClass.isAssignableFrom(UploadImageVM::class.java) -> UploadImageVM(application) as T
            modelClass.isAssignableFrom(MediaVM::class.java) -> MediaVM(
                application
            ) as T
            modelClass.isAssignableFrom(MediaDialogVM::class.java) -> MediaDialogVM(
                application
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }

}