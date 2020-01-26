package dev.gtcl.reddit.ui.main.fragments.posts.subreddit_selector

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.FragmentDialogSubredditsBinding
import dev.gtcl.reddit.ui.fragments.posts.subreddit_selector.SubredditStateAdapter

class SubredditSelectorDialogFragment: BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentDialogSubredditsBinding.inflate(inflater)
        val tabLayout = binding.tabLayout
        val viewPager = binding.viewPager
        viewPager.adapter = SubredditStateAdapter(this)
        TabLayoutMediator(tabLayout, viewPager){ tab, position ->
            tab.text = getText(when(position){
                0 -> R.string.mine_tab_label
                1 -> R.string.trending_tab_label
                2 -> R.string.popular_tab_label
                3 -> R.string.search_tab_label
                else -> throw NoSuchElementException("No such tab in the following position: $position")
            })
        }.attach()
        return binding.root
    }
}