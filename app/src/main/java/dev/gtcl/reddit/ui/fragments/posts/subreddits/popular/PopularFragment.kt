package dev.gtcl.reddit.ui.fragments.posts.subreddits.popular

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.databinding.FragmentSimpleRecyclerViewBinding
import dev.gtcl.reddit.ui.fragments.posts.ListingViewPagerFragment
import dev.gtcl.reddit.ui.fragments.posts.subreddits.SubredditOnClickListener

class PopularFragment : Fragment() {

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
        val model = (requireParentFragment().parentFragment as ListingViewPagerFragment).model

        val adapter = SubredditsPageListAdapter(subClickListener)
        binding.list.adapter = adapter

        model.popularSubreddits.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })
    }

}