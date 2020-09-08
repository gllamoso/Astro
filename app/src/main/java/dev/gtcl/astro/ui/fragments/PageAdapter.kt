package dev.gtcl.astro.ui.fragments

import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.astro.models.reddit.listing.Listing
import dev.gtcl.astro.models.reddit.listing.Post
import dev.gtcl.astro.ui.fragments.account.AccountFragment
import dev.gtcl.astro.ui.fragments.comments.CommentsFragment
import dev.gtcl.astro.ui.fragments.inbox.InboxFragment
import dev.gtcl.astro.ui.fragments.listing.ListingFragment
import kotlinx.android.parcel.Parcelize

class PageAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    private val pageStack = mutableListOf<Fragment>()

    override fun getItemCount() = pageStack.size

    override fun createFragment(position: Int) = pageStack[position]

    fun addPage(page: ViewPagerPage) {
        val fragment = when (page) {
            is ListingPage -> ListingFragment.newInstance(page.listing)
            is AccountPage -> AccountFragment.newInstance(page.user)
            is PostPage -> CommentsFragment.newInstance(page)
            is CommentsPage -> CommentsFragment.newInstance(
                page.url,
                page.expandReplies
            )
            InboxPage -> InboxFragment.newInstance()
        }
        pageStack.add(fragment)
        notifyItemInserted(pageStack.lastIndex)
    }

    fun popFragmentsGreaterThanPosition(currentPage: Int) {
        if (currentPage >= pageStack.lastIndex) {
            return
        }
        val itemsRemoved = pageStack.lastIndex - currentPage
        pageStack.subList(currentPage + 1, pageStack.size).clear()
        Handler(Looper.getMainLooper()).post {
            notifyItemRangeRemoved(pageStack.lastIndex + 1, itemsRemoved)
        }
    }

    fun addPages(pages: List<ViewPagerPage>) {
        pageStack.addAll(pages.map {
            when (it) {
                is ListingPage -> ListingFragment.newInstance(it.listing)
                is AccountPage -> AccountFragment.newInstance(it.user)
                is PostPage -> CommentsFragment.newInstance(it)
                is CommentsPage -> CommentsFragment.newInstance(
                    it.url,
                    it.expandReplies
                )
                InboxPage -> InboxFragment.newInstance()
            }
        })
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun containsItem(itemId: Long) = itemId <= pageStack.lastIndex
}

sealed class ViewPagerPage : Parcelable

@Parcelize
class ListingPage(
    val listing: Listing
) : ViewPagerPage()

@Parcelize
class AccountPage(
    val user: String?
) : ViewPagerPage()

@Parcelize
class PostPage(
    val post: Post,
    val position: Int
) : ViewPagerPage()

@Parcelize
class CommentsPage(
    val url: String,
    val expandReplies: Boolean
) : ViewPagerPage()

@Parcelize
object InboxPage : ViewPagerPage()