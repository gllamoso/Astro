package dev.gtcl.reddit.ui.fragments.subreddits

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.SubredditWhere
import dev.gtcl.reddit.ui.fragments.ListingScrollerFragment
import dev.gtcl.reddit.ui.fragments.subreddits.mine.MineFragment
import dev.gtcl.reddit.ui.fragments.subreddits.search.SearchFragment
import dev.gtcl.reddit.ui.fragments.subreddits.trending.TrendingListFragment

class SubredditStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment){

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> MineFragment.newInstance()
            1 -> TrendingListFragment.newInstance()
            2 -> ListingScrollerFragment.newInstance(SubredditWhere.POPULAR, 15) // Popular Subreddits
            3 -> SearchFragment.newInstance()
            else -> throw NoSuchElementException("Invalid position: $position")
        }
    }

}