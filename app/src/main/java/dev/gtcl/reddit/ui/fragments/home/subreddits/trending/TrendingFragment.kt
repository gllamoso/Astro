package dev.gtcl.reddit.ui.fragments.home.subreddits.trending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.databinding.FragmentSimpleRecyclerViewBinding
import dev.gtcl.reddit.ui.fragments.home.HomeViewPagerFragment
import dev.gtcl.reddit.ui.fragments.home.subreddits.SubredditOnClickListener

class TrendingFragment : Fragment() {
    private lateinit var binding: FragmentSimpleRecyclerViewBinding
    private lateinit var subClickListener: SubredditOnClickListener

    fun setSubredditOnClickListener(listener: SubredditOnClickListener){
        this.subClickListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSimpleRecyclerViewBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val model = (requireParentFragment().parentFragment as HomeViewPagerFragment).model
        val adapter = TrendingAdapter(subClickListener)

        binding.list.adapter = adapter

        model.trendingSubredditPosts.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })
    }
}