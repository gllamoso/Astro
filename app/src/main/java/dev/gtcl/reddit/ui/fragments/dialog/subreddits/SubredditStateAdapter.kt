package dev.gtcl.reddit.ui.fragments.dialog.subreddits

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.SubredditWhere
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.ui.fragments.SimpleListingScrollerFragment
import dev.gtcl.reddit.ui.fragments.dialog.subreddits.mine.MineFragment
import dev.gtcl.reddit.ui.fragments.dialog.subreddits.search.SearchFragment

class SubredditStateAdapter(fragment: Fragment): FragmentStateAdapter(fragment){

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when(position){
            0 -> MineFragment.newInstance()
            1 -> { // Trending Subreddits
                val trendingSubreddit = Subreddit( "", "trendingsubreddits", "", "", "", null, "")
                SimpleListingScrollerFragment.newInstance(
                    trendingSubreddit,
                    PostSort.HOT,
                    null,
                    15,
                    true)
            }
            2 -> SimpleListingScrollerFragment.newInstance(SubredditWhere.POPULAR, 20) // Popular Subreddits
            3 -> SearchFragment.newInstance()
            else -> throw NoSuchElementException("Invalid position: $position")
        }
    }

}