package dev.gtcl.reddit.ui.fragments

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.ui.fragments.comments.CommentsFragment
import dev.gtcl.reddit.ui.fragments.posts.PostListFragment

class MainFragmentStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
//    private val postListFragment = PostListFragment()
//    private val commentsFragment = CommentsFragment()

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> PostListFragment()
            1 -> CommentsFragment()
            else -> throw NoSuchElementException("Should have no fragment in the following position: $position")
        }
    }

}