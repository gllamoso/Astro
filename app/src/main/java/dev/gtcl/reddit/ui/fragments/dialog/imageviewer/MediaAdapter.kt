package dev.gtcl.reddit.ui.fragments.dialog.imageviewer

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.models.reddit.UrlType

class MediaAdapter(fragment: Fragment, private val url: String, private val urlType: UrlType, private val backupVideoUrl: String? = null): FragmentStateAdapter(fragment){
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> Fragment()
            1 -> MediaFragment.newInstance(url, urlType, backupVideoUrl)
            2 -> Fragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

}