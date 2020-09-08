package dev.gtcl.astro.ui.fragments.media

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.ui.fragments.media.list.MediaListFragment

class MediaSwipeToDismissAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    items: List<MediaURL>
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val fragments = listOf(Fragment(), MediaListFragment.newInstance(items), Fragment())

    override fun getItemCount() = fragments.size

    override fun createFragment(position: Int) = fragments[position]

}