package dev.gtcl.reddit.ui.fragments.account

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.ui.fragments.SimpleListingScrollerFragment
import dev.gtcl.reddit.ui.fragments.account.pages.about.UserAboutFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserBlockedFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserFriendsFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserGildedFragment
import java.lang.IllegalArgumentException

class AccountStateAdapter (fragment: Fragment, private val user: String?): FragmentStateAdapter(fragment){

    override fun getItemCount() = if(user == null) 11 else 5

    override fun createFragment(position: Int): Fragment {
        if(user == null){
            return when(position){
                0 -> UserAboutFragment.newInstance(user)
                1 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.OVERVIEW, PostSort.BEST, null, 15)
                2 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.SUBMITTED, PostSort.BEST, null, 15)
                3 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.COMMENTS, PostSort.BEST, null, 15)
                4 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.SAVED, PostSort.BEST, null, 15)
                5 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.HIDDEN, PostSort.BEST, null, 15)
                6 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.UPVOTED, PostSort.BEST, null, 15)
                7 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.DOWNVOTED, PostSort.BEST, null, 15)
                8 -> UserGildedFragment.newInstance()
                9 -> UserFriendsFragment.newInstance()
                10 -> UserBlockedFragment.newInstance()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }

        return when(position){
            0 -> UserAboutFragment.newInstance(user)
            1 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.OVERVIEW, PostSort.BEST, null, 15)
            2 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.SUBMITTED, PostSort.BEST, null, 15)
            3 -> SimpleListingScrollerFragment.newInstance(ProfileInfo.COMMENTS, PostSort.BEST, null, 40)
            4 -> UserGildedFragment.newInstance()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

}