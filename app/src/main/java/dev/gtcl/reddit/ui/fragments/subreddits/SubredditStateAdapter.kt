package dev.gtcl.reddit.ui.fragments.subreddits

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.ui.fragments.subreddits.mine.MineFragment
import dev.gtcl.reddit.ui.fragments.subreddits.popular.PopularFragment
import dev.gtcl.reddit.ui.fragments.subreddits.search.SearchFragment
import dev.gtcl.reddit.ui.fragments.subreddits.trending.TrendingFragment

class SubredditStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment){
    private val mineFragment = MineFragment()
    private val trendingFragment = TrendingFragment()
    private val popularFragment = PopularFragment()
    private val searchFragment = SearchFragment()

    fun setSubredditOnClickListener(listener: SubredditOnClickListener){
        mineFragment.setSubredditOnClickListener(listener)
        trendingFragment.setSubredditOnClickListener(listener)
        popularFragment.setSubredditOnClickListener(listener)
        searchFragment.setSubredditOnClickListener(listener)
    }

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> mineFragment
            1 -> trendingFragment
            2 -> popularFragment
            3 -> searchFragment
            else -> throw NoSuchElementException("Invalid position: $position")
        }
    }

}