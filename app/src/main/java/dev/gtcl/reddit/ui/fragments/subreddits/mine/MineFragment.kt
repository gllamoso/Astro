package dev.gtcl.reddit.ui.fragments.subreddits.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.databinding.FragmentSimpleRecyclerViewBinding
import dev.gtcl.reddit.posts.All
import dev.gtcl.reddit.posts.FrontPage
import dev.gtcl.reddit.posts.ListingType
import dev.gtcl.reddit.ui.fragments.MainFragment
import dev.gtcl.reddit.ui.fragments.subreddits.SubredditOnClickListener
import dev.gtcl.reddit.ui.fragments.subreddits.search.SubredditsListAdapter

class MineFragment : Fragment() {

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
        val model = (requireParentFragment().parentFragment as MainFragment).model

        val adapter = MultiAndSubsListAdapter(requireContext(), subClickListener)
        binding.list.adapter = adapter

        model.defaultSubreddits.observe(viewLifecycleOwner, Observer {
            val multis = listOf(FrontPage, All) // TODO
            adapter.submitLists(multis, it)
        })
    }

}
