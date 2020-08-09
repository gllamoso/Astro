package dev.gtcl.reddit.ui.fragments.account

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.ui.fragments.item_scroller.ItemScrollerFragment
import dev.gtcl.reddit.ui.fragments.account.pages.about.UserAboutFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserBlockedFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserFriendsFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserGildedFragment
import java.lang.IllegalArgumentException

class AccountStateAdapter (fragment: Fragment, private val user: String?): FragmentStateAdapter(fragment){

    override fun getItemCount() = if(user == null) 10 else 4

    override fun createFragment(position: Int): Fragment {
        if(user == null){
            return when(position){
                0 -> UserAboutFragment.newInstance(user)
                1 -> ItemScrollerFragment.newInstance(ProfileInfo.OVERVIEW, PostSort.BEST, null, 15)
                2 -> ItemScrollerFragment.newInstance(ProfileInfo.SUBMITTED, PostSort.BEST, null, 15)
                3 -> ItemScrollerFragment.newInstance(ProfileInfo.COMMENTS, PostSort.BEST, null, 15)
                4 -> ItemScrollerFragment.newInstance(ProfileInfo.SAVED, PostSort.BEST, null, 15)
                5 -> ItemScrollerFragment.newInstance(ProfileInfo.HIDDEN, PostSort.BEST, null, 15)
                6 -> ItemScrollerFragment.newInstance(ProfileInfo.UPVOTED, PostSort.BEST, null, 15)
                7 -> ItemScrollerFragment.newInstance(ProfileInfo.DOWNVOTED, PostSort.BEST, null, 15)
                8 -> UserFriendsFragment.newInstance()
                9 -> UserBlockedFragment.newInstance()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }

        return when(position){
            0 -> UserAboutFragment.newInstance(user)
            1 -> ItemScrollerFragment.newInstance(ProfileInfo.OVERVIEW, PostSort.BEST, null, 15, user)
            2 -> ItemScrollerFragment.newInstance(ProfileInfo.SUBMITTED, PostSort.BEST, null, 15, user)
            3 -> ItemScrollerFragment.newInstance(ProfileInfo.COMMENTS, PostSort.BEST, null, 15, user)
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

}