package dev.gtcl.reddit.ui.fragments.inbox

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.MessageWhere
import dev.gtcl.reddit.ui.fragments.SimpleListingScrollerFragment
import java.lang.IllegalArgumentException

class MessagesStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    override fun getItemCount() = 3
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> SimpleListingScrollerFragment.newInstance(MessageWhere.INBOX, 20)
            1 -> SimpleListingScrollerFragment.newInstance(MessageWhere.UNREAD, 20)
            2 -> SimpleListingScrollerFragment.newInstance(MessageWhere.SENT, 20)
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

}