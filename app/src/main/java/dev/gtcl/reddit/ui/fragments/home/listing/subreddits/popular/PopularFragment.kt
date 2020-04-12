package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.popular

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubredditWhere
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.ui.ListingAdapter
import dev.gtcl.reddit.ui.LoadMoreScrollListener
import dev.gtcl.reddit.ui.OnLoadMoreListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.database.asDomainModel

class PopularFragment : Fragment() {

    private lateinit var binding: FragmentRecyclerViewBinding
    private lateinit var listingAdapter: ListingAdapter
    private lateinit var subredditActions: SubredditActions

    val model: PopularViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(PopularViewModel::class.java)
    }

    fun setFragment(subredditActions: SubredditActions){
        this.subredditActions = subredditActions
        model.setListingInfo(SubredditWhere.POPULAR, 20)
        model.loadInitial()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecyclerViewBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        listingAdapter.notifyDataSetChanged()
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

        listingAdapter = ListingAdapter(subredditActions, {model.retry()}, {loadMoreScrollListener.lastItemReached()})
        binding.list.adapter = listingAdapter

        model.subscribedSubs.observe(viewLifecycleOwner, Observer {
            if(it != null)
                listingAdapter.setSubscribedSubs(it.asDomainModel())
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
            listingAdapter.setNetworkState(it)
        })
        model.initialListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                listingAdapter.loadInitial(it)
                model.loadInitialFinished()
            }
        })

        binding.list.addOnScrollListener(loadMoreScrollListener)

        model.additionalListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                listingAdapter.loadMore(it)
                model.loadAfterFinished()
                loadMoreScrollListener.finishedLoading()
            }
        })
        (binding.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }

}