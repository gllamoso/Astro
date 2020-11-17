package dev.gtcl.astro.ui.fragments.view_pager

import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.astro.ui.fragments.account.AccountFragment
import dev.gtcl.astro.ui.fragments.comments.CommentsFragment
import dev.gtcl.astro.ui.fragments.inbox.InboxFragment
import dev.gtcl.astro.ui.fragments.post_listing.PostListingFragment

class PageAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    private val pageStack = mutableListOf<Fragment>()

    override fun getItemCount() = pageStack.size

    override fun createFragment(position: Int) = pageStack[position]

    fun addPage(page: ViewPagerPage) {
        val fragment = when (page) {
            is ListingPage -> PostListingFragment.newInstance(page.postListing)
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
        Handler(Looper.getMainLooper()).post {  // To prevent IllegalStateException: FragmentManager is already executing transactions
            notifyItemRangeRemoved(pageStack.lastIndex + 1, itemsRemoved)
        }
    }

    fun addPages(pages: List<ViewPagerPage>) {
        pageStack.addAll(pages.map {
            when (it) {
                is ListingPage -> PostListingFragment.newInstance(it.postListing)
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