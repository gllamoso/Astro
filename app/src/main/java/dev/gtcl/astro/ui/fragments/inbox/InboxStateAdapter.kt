package dev.gtcl.astro.ui.fragments.inbox

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.astro.MessageWhere
import dev.gtcl.astro.ui.fragments.item_scroller.ItemScrollerFragment

class InboxStateAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragments = listOf(
        ItemScrollerFragment.newInstance(MessageWhere.INBOX, 15),
        ItemScrollerFragment.newInstance(MessageWhere.UNREAD, 15),
        ItemScrollerFragment.newInstance(
            MessageWhere.SENT, 15
        )
    )

    override fun getItemCount() = fragments.size
    override fun createFragment(position: Int) = fragments[position]

}