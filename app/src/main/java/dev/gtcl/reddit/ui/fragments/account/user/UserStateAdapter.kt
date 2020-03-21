package dev.gtcl.reddit.ui.fragments.account.user

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.ui.ViewPagerActions
import dev.gtcl.reddit.ui.fragments.account.user.about.UserAboutFragment
import dev.gtcl.reddit.ui.fragments.account.user.blocked.UserBlockedFragment
import dev.gtcl.reddit.ui.fragments.account.user.comments.UserCommentsFragment
import dev.gtcl.reddit.ui.fragments.account.user.downvoted.UserDownvotedFragment
import dev.gtcl.reddit.ui.fragments.account.user.friends.UserFriendsFragment
import dev.gtcl.reddit.ui.fragments.account.user.gilded.UserGildedFragment
import dev.gtcl.reddit.ui.fragments.account.user.hidden.UserHiddenFragment
import dev.gtcl.reddit.ui.fragments.account.user.overview.UserOverviewFragment
import dev.gtcl.reddit.ui.fragments.account.user.posts.UserPostsFragment
import dev.gtcl.reddit.ui.fragments.account.user.saved.UserSavedFragment
import dev.gtcl.reddit.ui.fragments.account.user.upvoted.UserUpvotedFragment
import java.lang.IllegalArgumentException

class UserStateAdapter (fragment: Fragment): FragmentStateAdapter(fragment){
    private val aboutFragment = UserAboutFragment()
    private val overviewFragment = UserOverviewFragment()
    private val postsFragment = UserPostsFragment()
    private val commentsFragment = UserCommentsFragment()
    private val savedFragment = UserSavedFragment()
    private val hiddenFragment = UserHiddenFragment()
    private val upvotedFragment = UserUpvotedFragment()
    private val downvotedFragment = UserDownvotedFragment()
    private val gildedFragment = UserGildedFragment()
    private val friendsFragment = UserFriendsFragment()
    private val blockedFragment = UserBlockedFragment()

    override fun getItemCount() = 11

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> aboutFragment
            1 -> overviewFragment
            2 -> postsFragment
            3 -> commentsFragment
            4 -> savedFragment
            5 -> hiddenFragment
            6 -> upvotedFragment
            7 -> downvotedFragment
            8 -> gildedFragment
            9 -> friendsFragment
            10 -> blockedFragment
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

}