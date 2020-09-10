package dev.gtcl.astro.ui.fragments.account

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.astro.PostSort
import dev.gtcl.astro.ProfileInfo
import dev.gtcl.astro.ui.fragments.account.pages.about.AccountAboutFragment
import dev.gtcl.astro.ui.fragments.account.pages.blocked.BlockedFragment
import dev.gtcl.astro.ui.fragments.account.pages.friends.FriendsFragment
import dev.gtcl.astro.ui.fragments.item_scroller.ItemScrollerFragment

class AccountFragmentAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    user: String?
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    val fragments = if (user == null) {
        listOf(
            AccountAboutFragment.newInstance(user),
            ItemScrollerFragment.newInstance(ProfileInfo.OVERVIEW, PostSort.BEST, null),
            ItemScrollerFragment.newInstance(ProfileInfo.SUBMITTED, PostSort.BEST, null),
            ItemScrollerFragment.newInstance(ProfileInfo.COMMENTS, PostSort.BEST, null),
            ItemScrollerFragment.newInstance(ProfileInfo.SAVED, PostSort.BEST, null),
            ItemScrollerFragment.newInstance(ProfileInfo.HIDDEN, PostSort.BEST, null),
            ItemScrollerFragment.newInstance(ProfileInfo.UPVOTED, PostSort.BEST, null),
            ItemScrollerFragment.newInstance(ProfileInfo.DOWNVOTED, PostSort.BEST, null),
            ItemScrollerFragment.newInstance(ProfileInfo.GILDED, PostSort.BEST, null),
            FriendsFragment.newInstance(),
            BlockedFragment.newInstance()
        )
    } else {
        listOf(
            AccountAboutFragment.newInstance(user),
            ItemScrollerFragment.newInstance(ProfileInfo.OVERVIEW, PostSort.BEST, null, user),
            ItemScrollerFragment.newInstance(ProfileInfo.SUBMITTED, PostSort.BEST, null, user),
            ItemScrollerFragment.newInstance(ProfileInfo.COMMENTS, PostSort.BEST, null, user),
            ItemScrollerFragment.newInstance(ProfileInfo.GILDED, PostSort.BEST, null, user)
        )
    }

    override fun getItemCount() = fragments.size

    override fun createFragment(position: Int) = fragments[position]

}