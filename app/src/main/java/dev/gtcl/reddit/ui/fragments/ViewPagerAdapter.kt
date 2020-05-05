package dev.gtcl.reddit.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.POST_KEY
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.ui.fragments.account.AccountFragment
import dev.gtcl.reddit.ui.fragments.comments.CommentsFragment
import dev.gtcl.reddit.ui.fragments.home.listing.ListingFragment
import java.util.*

class ViewPagerAdapter(fragment: Fragment, startingFragment: StartingViewPagerFragments): FragmentStateAdapter(fragment){
    val fragments = Stack<Fragment>()

    init {
        when(startingFragment){
            StartingViewPagerFragments.LISTING -> fragments.add(ListingFragment())
            StartingViewPagerFragments.USER -> fragments.add(AccountFragment())
        }
    }

    override fun getItemCount() = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun addCommentsPage(post: Post){
        val commentsFragment = CommentsFragment()
        val bundle = Bundle()
        bundle.putParcelable(POST_KEY, post)
        commentsFragment.arguments = bundle
        fragments.add(commentsFragment)
        notifyItemInserted(fragments.size - 1)
    }

    fun popFragments(currentPage: Int){
        var itemsRemoved = 0
        while(currentPage < fragments.size - 1) {
            fragments.pop()
            itemsRemoved++
        }
        notifyItemRangeRemoved(fragments.size - 1, itemsRemoved)
    }
}

enum class StartingViewPagerFragments{
    LISTING,
    USER
}