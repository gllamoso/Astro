package dev.gtcl.reddit.ui.fragments.dialog.subreddits

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.mine.MineFragment
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.popular.PopularFragment
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.search.SearchFragment
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.trending.TrendingFragment

class SubredditStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment){

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> MineFragment()
            1 -> TrendingFragment()
            2 -> PopularFragment()
            3 -> SearchFragment()
            else -> throw NoSuchElementException("Invalid position: $position")
        }
    }

}