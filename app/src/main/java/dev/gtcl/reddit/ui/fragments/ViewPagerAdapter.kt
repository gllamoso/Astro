package dev.gtcl.reddit.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.network.Post
import dev.gtcl.reddit.ui.fragments.account.user.UserFragment
import dev.gtcl.reddit.ui.ViewPagerActions
import dev.gtcl.reddit.ui.fragments.comments.CommentsFragment
import dev.gtcl.reddit.ui.fragments.comments.PRELOADED_POST_KEY
import dev.gtcl.reddit.ui.fragments.posts.listing.ListingFragment

class ViewPagerAdapter(fragment: Fragment, startingFragment: StartingViewPagerFragments, private val actions: ViewPagerActions): FragmentStateAdapter(fragment){
    val fragments = ArrayList<Fragment>()

    init {
        when(startingFragment){
            StartingViewPagerFragments.LISTING -> fragments.add(ListingFragment())
            StartingViewPagerFragments.USER -> fragments.add(UserFragment())
        }
    }

    override fun getItemCount() = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun addCommentsPage(post: Post){
        val commentsFragment = CommentsFragment()
        val bundle = Bundle()
        bundle.putParcelable(PRELOADED_POST_KEY, post)
        commentsFragment.arguments = bundle
        fragments.add(commentsFragment)
        notifyItemInserted(fragments.size - 1)
    }

    fun popFragment(){
        fragments.removeAt(fragments.size - 1)
        notifyItemRemoved(fragments.size) // TODO: Cannot call this method in a scroll callback. Scroll callbacks mightbe run during a measure & layout pass where you cannot change theRecyclerView data. Any method call that might change the structureof the RecyclerView or the adapter contents should be postponed tothe next frame.
    }
}

enum class StartingViewPagerFragments{
    LISTING,
    USER
}