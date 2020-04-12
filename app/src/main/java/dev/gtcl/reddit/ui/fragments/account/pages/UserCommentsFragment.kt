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
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.listings.ProfileListing
import dev.gtcl.reddit.ui.*
import dev.gtcl.reddit.ui.fragments.LoadMoreScrollViewModel

class UserCommentsFragment : Fragment() {
    private lateinit var binding: FragmentRecyclerViewBinding

    val model: LoadMoreScrollViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(LoadMoreScrollViewModel::class.java)
    }

    private lateinit var postActions: PostActions
    fun setFragment(postActions: PostActions, user: String?){
        this.postActions = postActions
        model.setUser(user)
        model.setListingInfo(ProfileListing(ProfileInfo.COMMENTS), PostSort.BEST, null, 40)
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
            object: OnLoadMoreListener{
                override fun loadMore() {
                    model.loadAfter()
                }
            }
        )

        val adapter = ListingAdapter(postActions, {model.retry()}, {loadMoreScrollListener.lastItemReached()})
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