package dev.gtcl.reddit.ui.fragments

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.models.reddit.ListingType
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.ui.fragments.account.AccountFragment
import dev.gtcl.reddit.ui.fragments.comments.CommentsFragment
import dev.gtcl.reddit.ui.fragments.home.listing.ListingFragment
import kotlinx.android.parcel.Parcelize
import java.util.*

class PageAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    private var pageStack = Stack<PageTypes>()

    override fun getItemCount() = pageStack.size

    override fun createFragment(position: Int): Fragment {
        return when(val pageType = pageStack[position]){
            is ListingPage -> ListingFragment.newInstance(pageType.listingType)
            is AccountPage -> AccountFragment.newInstance(pageType.user)
            is PostPage -> CommentsFragment.newInstance(pageType.post)
        }
    }

    fun addAccountPage(user: String?){
        pageStack.add(AccountPage(user))
        notifyItemInserted(pageStack.size - 1)
    }

    fun addPostPage(post: Post){
        pageStack.add(PostPage(post))
        notifyItemInserted(pageStack.size - 1)
    }

    fun addListingPage(listingType: ListingType){
        pageStack.add(ListingPage(listingType))
        notifyItemInserted(pageStack.size - 1)
    }

    fun popFragmentsGreaterThanPosition(currentPage: Int){
        var itemsRemoved = 0
        while(currentPage < pageStack.size - 1) {
            pageStack.pop()
            itemsRemoved++
        }
        notifyItemRangeRemoved(pageStack.size - 1, itemsRemoved)
    }

    fun getPageStack(): Stack<PageTypes> = pageStack

    fun setPageStack(pageStack: Stack<PageTypes>){
        this.pageStack = pageStack
        notifyDataSetChanged()
    }
}

sealed class PageTypes: Parcelable
@Parcelize
class ListingPage(
    val listingType: ListingType
): PageTypes()
@Parcelize
class AccountPage(
    val user: String?
): PageTypes()
@Parcelize
class PostPage(
    val post: Post
): PageTypes()