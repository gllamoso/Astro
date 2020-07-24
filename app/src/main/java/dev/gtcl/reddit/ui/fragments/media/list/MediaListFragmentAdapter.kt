package dev.gtcl.reddit.ui.fragments.media.list

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.ui.fragments.media.list.item.MediaItemFragment

class MediaListFragmentAdapter(fragment: Fragment, private val items: List<MediaURL>): FragmentStateAdapter(fragment){
    override fun getItemCount() = items.size
    override fun createFragment(position: Int) = MediaItemFragment.newInstance(items[position])
}