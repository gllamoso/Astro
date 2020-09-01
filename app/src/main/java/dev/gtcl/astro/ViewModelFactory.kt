package dev.gtcl.astro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.account.AccountFragmentVM
import dev.gtcl.astro.ui.fragments.ViewPagerVM
import dev.gtcl.astro.ui.fragments.comments.CommentsVM
import dev.gtcl.astro.ui.fragments.listing.ListingVM
import dev.gtcl.astro.ui.fragments.item_scroller.ItemScrollerVM
import dev.gtcl.astro.ui.fragments.account.pages.about.AccountAboutVM
import dev.gtcl.astro.ui.fragments.account.pages.blocked.BlockedVM
import dev.gtcl.astro.ui.fragments.account.pages.friends.FriendsVM
import dev.gtcl.astro.ui.fragments.create_post.CreatePostVM
import dev.gtcl.astro.ui.fragments.create_post.type.UploadImageVM
import dev.gtcl.astro.ui.fragments.flair.FlairListVM
import dev.gtcl.astro.ui.fragments.inbox.ComposeVM
import dev.gtcl.astro.ui.fragments.manage.ManagePostVM
import dev.gtcl.astro.ui.fragments.media.MediaDialogVM
import dev.gtcl.astro.ui.fragments.media.list.item.MediaVM
import dev.gtcl.astro.ui.fragments.signin.SignInVM
import dev.gtcl.astro.ui.fragments.splash.SplashVM
import dev.gtcl.astro.ui.fragments.subscriptions.SubscriptionsVM
import dev.gtcl.astro.ui.fragments.multireddits.MultiRedditVM
import dev.gtcl.astro.ui.fragments.reply_or_edit.ReplyOrEditVM
import dev.gtcl.astro.ui.fragments.report.ReportVM
import dev.gtcl.astro.ui.fragments.rules.RulesVM
import dev.gtcl.astro.ui.fragments.search.SearchVM
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val application: AstroApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SplashVM::class.java) -> SplashVM(application) as T
            modelClass.isAssignableFrom(SignInVM::class.java) -> SignInVM(application) as T
            modelClass.isAssignableFrom(ListingVM::class.java) -> ListingVM(application) as T
            modelClass.isAssignableFrom(ViewPagerVM::class.java) -> ViewPagerVM() as T
            modelClass.isAssignableFrom(CommentsVM::class.java) -> CommentsVM(application) as T
            modelClass.isAssignableFrom(MainActivityVM::class.java) -> MainActivityVM(application) as T
            modelClass.isAssignableFrom(AccountFragmentVM::class.java) -> AccountFragmentVM(application) as T
            modelClass.isAssignableFrom(AccountAboutVM::class.java) -> AccountAboutVM(application) as T
            modelClass.isAssignableFrom(ItemScrollerVM::class.java) -> ItemScrollerVM(application) as T
            modelClass.isAssignableFrom(SubscriptionsVM::class.java) -> SubscriptionsVM(application) as T
            modelClass.isAssignableFrom(SearchVM::class.java) -> SearchVM(application) as T
            modelClass.isAssignableFrom(MultiRedditVM::class.java) -> MultiRedditVM(application) as T
            modelClass.isAssignableFrom(CreatePostVM::class.java) -> CreatePostVM(application) as T
            modelClass.isAssignableFrom(UploadImageVM::class.java) -> UploadImageVM() as T
            modelClass.isAssignableFrom(MediaVM::class.java) -> MediaVM(application) as T
            modelClass.isAssignableFrom(MediaDialogVM::class.java) -> MediaDialogVM(application) as T
            modelClass.isAssignableFrom(ReplyOrEditVM::class.java) -> ReplyOrEditVM(application) as T
            modelClass.isAssignableFrom(ComposeVM::class.java) -> ComposeVM(application) as T
            modelClass.isAssignableFrom(FriendsVM::class.java) -> FriendsVM(application) as T
            modelClass.isAssignableFrom(BlockedVM::class.java) -> BlockedVM(application) as T
            modelClass.isAssignableFrom(ReportVM::class.java) -> ReportVM(application) as T
            modelClass.isAssignableFrom(RulesVM::class.java) -> RulesVM(application) as T
            modelClass.isAssignableFrom(FlairListVM::class.java) -> FlairListVM(application) as T
            modelClass.isAssignableFrom(ManagePostVM::class.java) -> ManagePostVM() as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }

}