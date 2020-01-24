package dev.gtcl.reddit.ui.subreddits

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import dev.gtcl.reddit.ui.subreddits.tabs.mine.MineFragment
import dev.gtcl.reddit.ui.subreddits.tabs.popular.PopularFragment
import dev.gtcl.reddit.ui.subreddits.tabs.search.SearchFragment
import dev.gtcl.reddit.ui.subreddits.tabs.trending.TrendingFragment

private const val TAB_COUNT = 4
class PageAdapter(fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        return when(position){
            0 -> MineFragment()
            1 -> TrendingFragment()
            2 -> PopularFragment()
            else -> SearchFragment()
        }
    }

    override fun getCount(): Int {
        return TAB_COUNT
    }

}