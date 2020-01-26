package dev.gtcl.reddit.ui.main.fragments.posts.subreddit_selector

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.ui.subreddits.tabs.mine.MineFragment
import dev.gtcl.reddit.ui.subreddits.tabs.popular.PopularFragment
import dev.gtcl.reddit.ui.subreddits.tabs.search.SearchFragment
import dev.gtcl.reddit.ui.subreddits.tabs.trending.TrendingFragment

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