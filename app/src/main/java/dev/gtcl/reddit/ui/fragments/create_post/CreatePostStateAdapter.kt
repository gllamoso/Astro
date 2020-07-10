package dev.gtcl.reddit.ui.fragments.create_post

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.ui.fragments.create_post.type.LinkFragment
import dev.gtcl.reddit.ui.fragments.create_post.type.TextFragment
import dev.gtcl.reddit.ui.fragments.create_post.type.UploadImageFragment
import java.lang.IllegalArgumentException

class CreatePostStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment{
        return when(position){
            0 -> TextFragment()
            1 -> UploadImageFragment()
            2 -> LinkFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}