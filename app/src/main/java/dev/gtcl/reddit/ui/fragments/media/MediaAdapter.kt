package dev.gtcl.reddit.ui.fragments.media

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.models.reddit.UrlType

class MediaAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> Fragment()
            1 -> MediaFragment.newInstance()
            2 -> Fragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

}