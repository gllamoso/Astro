package dev.gtcl.reddit.ui.fragments.posts

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.network.Post
import dev.gtcl.reddit.ui.fragments.posts.comments.CommentsFragment
import dev.gtcl.reddit.ui.fragments.posts.listing.ListingFragment

class MainFragmentStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    private val postListFragment = ListingFragment()
    private val commentsFragment = CommentsFragment()

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> postListFragment
            1 -> commentsFragment
            else -> throw NoSuchElementException("Should have no fragment in the following position: $position")
        }
    }

    fun setCommentPage(post: Post){
        commentsFragment.setPost(post)
    }

    fun resetCommentPage(){
        commentsFragment.reset()
    }

    fun setCommentsFragmentListener(listener: MainFragmentViewPagerListener){
        commentsFragment.setMainFragmentListener(listener)
    }

    fun setOnPostClickedListener(onPostClicked: (Post) -> (Unit)){
        postListFragment.setPostSelectionListener(onPostClicked)
    }
}