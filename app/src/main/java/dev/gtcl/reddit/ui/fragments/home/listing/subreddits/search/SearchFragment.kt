package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.database.asDomainModel

class SearchFragment : Fragment() {
    private lateinit var binding: FragmentRecyclerViewBinding
    private lateinit var subredditActions: SubredditActions
    private lateinit var searchAdapter: SearchAdapter

    val model: SearchViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SearchViewModel::class.java)
    }

    fun setFragment(subredditActions: SubredditActions){
        this.subredditActions = subredditActions
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecyclerViewBinding.inflate(inflater)
        binding.list.visibility = View.GONE
        binding.noResultsText.visibility = View.VISIBLE
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        searchAdapter = SearchAdapter(subredditActions)
        binding.list.adapter = searchAdapter

        model.subscribedSubs.observe(viewLifecycleOwner, Observer {
            searchAdapter.submitSubscriptions(it.asDomainModel())
        })

        model.searchedSubreddits.observe(viewLifecycleOwner, Observer {
            if(it != null){
                searchAdapter.submitList(it)
                binding.list.smoothScrollToPosition(0)
                binding.list.visibility = if(it.isEmpty()) View.GONE else View.VISIBLE
                binding.noResultsText.visibility = if(it.isEmpty()) View.VISIBLE else View.GONE
                model.searchComplete()
            }
        })
    }

    fun searchSubreddit(query: String){
        searchAdapter.submitList(listOf())
        model.searchSubreddits(query)
    }
}