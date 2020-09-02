package dev.gtcl.astro.ui.fragments

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.fragments.account.AccountFragment
import dev.gtcl.astro.ui.fragments.comments.CommentsFragment
import dev.gtcl.astro.ui.fragments.inbox.InboxFragment
import dev.gtcl.astro.ui.fragments.listing.ListingFragment
import kotlinx.android.parcel.Parcelize

class PageAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    private var pageStack = mutableListOf<ViewPagerPage>()

    override fun getItemCount() = pageStack.size

    override fun createFragment(position: Int): Fragment {
        return when(val pageType = pageStack[position]){
            is ListingPage -> ListingFragment.newInstance(pageType.listing)
            is AccountPage -> AccountFragment.newInstance(pageType.user)
            is PostPage -> CommentsFragment.newInstance(pageType)
            is CommentsPage -> CommentsFragment.newInstance(pageType.url, pageType.expandReplies)
            InboxPage -> InboxFragment.newInstance()
        }
    }

    fun addPage(pageType: ViewPagerPage){
        pageStack.add(pageType)
        notifyItemInserted(pageStack.lastIndex)
    }

    fun popFragmentsGreaterThanPosition(currentPage: Int){
        val itemsRemoved = pageStack.lastIndex - currentPage
        pageStack.subList(currentPage + 1, pageStack.size).clear()
        notifyItemRangeRemoved(pageStack.lastIndex + 1, itemsRemoved)
    }

    fun setPageStack(pages: MutableList<ViewPagerPage>){
        pageStack = pages
        notifyDataSetChanged()
    }
}

sealed class ViewPagerPage: Parcelable
@Parcelize
class ListingPage(
    val listing: Listing
): ViewPagerPage()
@Parcelize
class AccountPage(
    val user: String?
): ViewPagerPage()
@Parcelize
class PostPage(
    val post: Post,
    val position: Int
): ViewPagerPage()
@Parcelize
class CommentsPage(
    val url: String,
    val expandReplies: Boolean
): ViewPagerPage()
@Parcelize
object InboxPage : ViewPagerPage()