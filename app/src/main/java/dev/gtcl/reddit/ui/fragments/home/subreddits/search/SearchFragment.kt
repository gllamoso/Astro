package dev.gtcl.reddit.ui.fragments.home.subreddits.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.ui.fragments.home.HomeFragment
import dev.gtcl.reddit.ui.fragments.home.subreddits.SubredditOnClickListener

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentRecyclerViewBinding
    lateinit var subClickListener: SubredditOnClickListener

    fun setSubredditOnClickListener(listener: SubredditOnClickListener){
        this.subClickListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecyclerViewBinding.inflate(inflater)
        binding.list.visibility = View.GONE
        binding.noResultsText.visibility = View.VISIBLE
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val model = (requireParentFragment().parentFragment as HomeFragment).model

        // Set adapter
        val adapter = SubredditsListAdapter(subClickListener)
        binding.list.adapter = adapter

        model.searchSubreddits.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it ?: listOf())
            binding.list.smoothScrollToPosition(0)
            if(it.isNullOrEmpty()) {
                binding.list.visibility = View.GONE
                binding.noResultsText.visibility = View.VISIBLE
            }
            else{
                binding.list.visibility = View.VISIBLE
                binding.noResultsText.visibility = View.GONE
            }
        })
    }
}