package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.trending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.listings.Subreddit
import dev.gtcl.reddit.listings.SubredditListing
import dev.gtcl.reddit.ui.LoadMoreScrollListener
import dev.gtcl.reddit.ui.OnLoadMoreListener
import dev.gtcl.reddit.ui.fragments.LoadMoreScrollViewModel
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.SubredditActions

class TrendingFragment : Fragment() {
    private lateinit var binding: FragmentRecyclerViewBinding
    private lateinit var subredditActions: SubredditActions

    val model: LoadMoreScrollViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(LoadMoreScrollViewModel::class.java)
    }

    fun setFragment(subredditActions: SubredditActions){
        this.subredditActions = subredditActions
        model.setListingInfo(
            SubredditListing(Subreddit( "", "", "trendingsubreddits", null, title = null)),
            PostSort.HOT,
            null,
            15)
        model.loadInitial()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecyclerViewBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val loadMoreScrollListener = LoadMoreScrollListener(
            binding.list.layoutManager as GridLayoutManager,
            object: OnLoadMoreListener {
                override fun loadMore() {
                    model.loadAfter()
                }
            }
        )

        val adapter = TrendingAdapter(subredditActions, {model.retry()}, {loadMoreScrollListener.lastItemReached()})
        binding.list.adapter = adapter

        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })
        model.initialListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.loadInitial(it.toTrendingPosts())
                model.loadInitialFinished()
            }
        })

        binding.list.addOnScrollListener(loadMoreScrollListener)

        model.additionalListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.loadMore(it.toTrendingPosts())
                model.loadAfterFinished()
                loadMoreScrollListener.finishedLoading()
            }
        })
        (binding.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }
}