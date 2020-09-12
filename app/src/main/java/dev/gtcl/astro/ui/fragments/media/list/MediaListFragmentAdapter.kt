package dev.gtcl.astro.ui.fragments.media.list

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.ui.fragments.media.list.item.MediaFragment

class MediaListFragmentAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    items: List<MediaURL>,
    playWhenReady: Boolean
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    val fragments = items.map { MediaFragment.newInstance(it, playWhenReady) }
    override fun getItemCount() = fragments.size
    override fun createFragment(position: Int) = fragments[position]
}