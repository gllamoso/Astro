package dev.gtcl.reddit.ui.fragments

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.models.reddit.ListingType
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.ui.fragments.account.AccountFragment
import dev.gtcl.reddit.ui.fragments.comments.CommentsFragment
import dev.gtcl.reddit.ui.fragments.listing.ListingFragment
import dev.gtcl.reddit.ui.fragments.inbox.MessagesFragment
import kotlinx.android.parcel.Parcelize
import kotlin.collections.ArrayList

class PageAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    private var pageStack = ArrayList<PageType>()

    override fun getItemCount() = pageStack.size

    override fun createFragment(position: Int): Fragment {
        return when(val pageType = pageStack[position]){
            is ListingPage -> ListingFragment.newInstance(pageType.listingType)
            is AccountPage -> AccountFragment.newInstance(pageType.user)
            is PostPage -> CommentsFragment.newInstance(pageType.post)
            is MessagesPage -> MessagesFragment()
        }
    }

    fun addAccountPage(user: String?){
        pageStack.add(AccountPage(user))
        notifyItemInserted(pageStack.lastIndex)
    }

    fun addPostPage(post: Post){
        pageStack.add(PostPage(post))
        notifyItemInserted(pageStack.lastIndex)
    }

    fun addListingPage(listingType: ListingType){
        pageStack.add(ListingPage(listingType))
        notifyItemInserted(pageStack.lastIndex)
    }

    fun addPage(pageType: PageType){
        pageStack.add(pageType)
        notifyItemInserted(pageStack.lastIndex)
    }

    fun popFragmentsGreaterThanPosition(currentPage: Int){
        val itemsRemoved = pageStack.lastIndex - currentPage
        pageStack.subList(currentPage + 1, pageStack.size).clear()
        notifyItemRangeRemoved(pageStack.lastIndex, itemsRemoved)
    }

    fun getPageStack(): ArrayList<PageType> = pageStack

    fun setPageStack(pages: ArrayList<PageType>){
        pageStack = pages
    }
}

sealed class PageType: Parcelable
@Parcelize
class ListingPage(
    val listingType: ListingType
): PageType()
@Parcelize
class AccountPage(
    val user: String?
): PageType()
@Parcelize
class PostPage(
    val post: Post
): PageType()
@Parcelize
object MessagesPage: PageType()