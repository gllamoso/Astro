package dev.gtcl.reddit.ui.fragments.media.test

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.ui.fragments.media.test.list.MediaListFragment

class MediaSwipeToDismissAdapter(fragment: Fragment, private val items: List<MediaURL>): FragmentStateAdapter(fragment){

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> Fragment()
            1 -> MediaListFragment.newInstance(items)
            2 -> Fragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }

    }

}