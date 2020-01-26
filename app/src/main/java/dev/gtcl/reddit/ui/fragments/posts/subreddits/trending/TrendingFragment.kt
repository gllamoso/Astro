package dev.gtcl.reddit.ui.fragments.posts.subreddits.trending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.databinding.FragmentSimpleRecyclerViewBinding
import dev.gtcl.reddit.ui.fragments.MainFragment
import dev.gtcl.reddit.ui.fragments.posts.subreddits.SubredditSelectorDialogFragment

class TrendingFragment : Fragment() {
    private lateinit var binding: FragmentSimpleRecyclerViewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSimpleRecyclerViewBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val model = (parentFragment!!.parentFragment as MainFragment).model
        val adapter = TrendingAdapter(TrendingAdapter.OnClickListener {
            model.getPosts(it)
            (parentFragment!! as SubredditSelectorDialogFragment).dismiss()
        })

        binding.list.adapter = adapter

        model.trendingSubredditPosts.observe(this, Observer {
            adapter.submitList(it)
        })
    }
}