package dev.gtcl.reddit.ui.fragments.account

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.ui.fragments.account.pages.UserAboutFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserBlockedFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserCommentsFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserDownvotedFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserFriendsFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserGildedFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserHiddenFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserOverviewFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserPostsFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserSavedFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserUpvotedFragment
import java.lang.IllegalArgumentException

class AccountStateAdapter (fragment: Fragment, private val isCurrentUser: Boolean): FragmentStateAdapter(fragment){

    override fun getItemCount() = if(isCurrentUser) 11 else 5

    override fun createFragment(position: Int): Fragment {
        if(isCurrentUser){
            return when(position){
                0 -> UserAboutFragment()
                1 -> UserOverviewFragment()
                2 -> UserPostsFragment()
                3 -> UserCommentsFragment()
                4 -> UserSavedFragment()
                5 -> UserHiddenFragment()
                6 -> UserUpvotedFragment()
                7 -> UserDownvotedFragment()
                8 -> UserGildedFragment()
                9 -> UserFriendsFragment()
                10 -> UserBlockedFragment()
                else -> throw IllegalArgumentException("Invalid position $position")
            }
        }

        return when(position){
            0 -> UserAboutFragment()
            1 -> UserOverviewFragment()
            2 -> UserPostsFragment()
            3 -> UserCommentsFragment()
            4 -> UserGildedFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

}