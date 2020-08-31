package dev.gtcl.astro.ui.fragments.create_post

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.astro.ui.fragments.create_post.type.CreatePostLinkFragment
import dev.gtcl.astro.ui.fragments.create_post.type.CreatePostTextFragment
import dev.gtcl.astro.ui.fragments.create_post.type.CreatePostImageFragment
import java.lang.IllegalArgumentException

class CreatePostStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment{
        return when(position){
            0 -> CreatePostTextFragment()
            1 -> CreatePostImageFragment()
            2 -> CreatePostLinkFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}