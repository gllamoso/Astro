package dev.gtcl.reddit.ui.main.fragments

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.ui.main.fragments.comments.PostDetailsFragment
import dev.gtcl.reddit.ui.main.fragments.posts.PostListFragment

class MainFragmentStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    private val postListFragment = PostListFragment()
    private val postDetailsFragment = PostDetailsFragment()

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> postListFragment
            1 -> postDetailsFragment
            else -> throw NoSuchElementException("Should have no fragment in the following position: $position")
        }
    }

}