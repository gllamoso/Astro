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
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.database.asDomainModel

class TrendingFragment : Fragment() {
    private lateinit var binding: FragmentRecyclerViewBinding
    private lateinit var subredditActions: SubredditActions
    private lateinit var trendingAdapter: TrendingAdapter

    val model: TrendingViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(TrendingViewModel::class.java)
    }

    fun setFragment(subredditActions: SubredditActions){
        this.subredditActions = subredditActions
        model.setListingInfo(
            SubredditListing(Subreddit( "", "trendingsubreddits", null, "")),
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

    override fun onResume() {
        super.onResume()
        trendingAdapter.notifyDataSetChanged()
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

        trendingAdapter = TrendingAdapter(subredditActions, {model.retry()}, {loadMoreScrollListener.lastItemReached()})
        binding.list.adapter = trendingAdapter

        model.subscribedSubs.observe(viewLifecycleOwner, Observer {
            if(it != null)
                trendingAdapter.setSubscribedSubs(it.asDomainModel())
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
            trendingAdapter.setNetworkState(it)
        })
        model.initialListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                trendingAdapter.loadInitial(it)
                model.loadInitialFinished()
            }
        })

        binding.list.addOnScrollListener(loadMoreScrollListener)

        model.additionalListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                trendingAdapter.loadMore(it)
                model.loadAfterFinished()
                loadMoreScrollListener.finishedLoading()
            }
        })
        (binding.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }
}