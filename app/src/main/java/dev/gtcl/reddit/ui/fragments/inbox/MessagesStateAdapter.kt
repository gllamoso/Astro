package dev.gtcl.reddit.ui.fragments.inbox

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.MessageWhere
import dev.gtcl.reddit.ui.fragments.ListingScrollerFragment
import java.lang.IllegalArgumentException

class MessagesStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    override fun getItemCount() = 3
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> ListingScrollerFragment.newInstance(MessageWhere.INBOX, 15)
            1 -> ListingScrollerFragment.newInstance(MessageWhere.UNREAD, 15)
            2 -> ListingScrollerFragment.newInstance(MessageWhere.SENT, 15)
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

}