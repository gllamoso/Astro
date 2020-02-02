package dev.gtcl.reddit.ui.fragments.posts.subreddits.popular

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.databinding.FragmentSimpleRecyclerViewBinding
import dev.gtcl.reddit.ui.fragments.MainFragment
import dev.gtcl.reddit.ui.fragments.posts.subreddits.SubredditSelectorDialogFragment

class PopularFragment : Fragment() {

    private lateinit var binding: FragmentSimpleRecyclerViewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSimpleRecyclerViewBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val model = (parentFragment!!.parentFragment as MainFragment).model

        val adapter = SubredditsPageListAdapter(
            SubredditsPageListAdapter.OnClickListener {
                model.fetchPosts(it)
                (parentFragment!! as SubredditSelectorDialogFragment).dismiss()
            })
        {model.retryFetchPopularPosts()}

        binding.list.adapter = adapter

        model.popularSubreddits.observe(this, Observer {
            adapter.submitList(it)
        })
    }

}