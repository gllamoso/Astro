package dev.gtcl.reddit.ui.fragments.account.pages

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
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.models.reddit.ProfileListing
import dev.gtcl.reddit.ui.LoadMoreScrollListener
import dev.gtcl.reddit.ui.OnLoadMoreListener
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.ui.fragments.LoadMoreScrollViewModel

class UserDownvotedFragment : Fragment() {
    private lateinit var binding: FragmentRecyclerViewBinding

    val model: LoadMoreScrollViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(LoadMoreScrollViewModel::class.java)
    }

    private lateinit var postActions: PostActions
    fun setFragment(postActions: PostActions){
        this.postActions = postActions
        model.setListingInfo(ProfileListing(ProfileInfo.DOWNVOTED), PostSort.BEST, null, 40)
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

        val adapter = dev.gtcl.reddit.ui.ListingAdapter(
            postActions,
            { model.retry() },
            { loadMoreScrollListener.lastItemReached() })
        binding.list.adapter = adapter

        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })
        model.initialListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.loadInitial(it)
                model.loadInitialFinished()
            }
        })

        binding.list.addOnScrollListener(loadMoreScrollListener)

        model.additionalListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.loadMore(it)
                model.loadAfterFinished()
                loadMoreScrollListener.finishedLoading()
            }
        })
        (binding.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    }
}