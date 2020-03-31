package dev.gtcl.reddit.ui.fragments.posts.listing

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentPostListBinding
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.network.Post
import dev.gtcl.reddit.listings.FrontPage
import dev.gtcl.reddit.listings.ListingType
import dev.gtcl.reddit.ui.*
import dev.gtcl.reddit.ui.fragments.posts.listing.sort_sheet.SortSheetDialogFragment
import dev.gtcl.reddit.ui.fragments.posts.subreddits.SubredditOnClickListener
import dev.gtcl.reddit.ui.fragments.posts.subreddits.SubredditSelectorDialogFragment
import dev.gtcl.reddit.ui.fragments.posts.listing.time_period_sheet.TimePeriodSheetDialogFragment

class ListingFragment : Fragment(), PostActions {

    private lateinit var binding: FragmentPostListBinding
    private lateinit var adapter: dev.gtcl.reddit.ListingAdapter

    private lateinit var viewPagerActions: ViewPagerActions
    fun setViewPagerActions(viewPagerActions: ViewPagerActions){
        this.viewPagerActions = viewPagerActions
    }

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }
    val model: ListingViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ListingViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(!::binding.isInitialized)
            setupFragment(inflater)
        return binding.root
    }

    private fun setupFragment(inflater: LayoutInflater){
        binding = FragmentPostListBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model

        //TODO: Add
//        val subredditSelected = savedInstanceState?.getString(KEY_SUBREDDIT) ?: DEFAULT_SUBREDDIT
//        model.getPosts(Subreddit(displayName = subredditSelected))

        // TODO: Update. Wrap with observer, observing a refresh live data
        parentModel.fetchData.observe(viewLifecycleOwner, Observer{
            if(it) {
                model.loadInitial(FrontPage)
            }
        })

        binding.toolbar.setNavigationOnClickListener {
            parentModel.openDrawer()
        }

        setRecyclerView()
        setSwipeToRefresh()
        setBottomAppbarClickListeners()
    }

    private fun setRecyclerView() {
//        val adapter = ListingAdapter({model.retry()}, this)
        adapter = dev.gtcl.reddit.ListingAdapter({model.retry()}, this)


        binding.list.adapter = adapter
        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })

        parentModel.allReadPosts.observe(viewLifecycleOwner, Observer {
            adapter.setReadSubs(it)
        })

        model.initialListing.observe(viewLifecycleOwner, Observer {
            if(it != null) {
//                adapter.loadInitial(test)
                adapter.loadInitial(it)
                model.loadInitialFinished()
            }
        })

        val loadMoreScrollListener = LoadMoreScrollListener(binding.list.layoutManager as GridLayoutManager, object : OnLoadMoreListener{
            override fun loadMore() {
                model.loadAfter()
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

    private fun setSwipeToRefresh() {
        model.refreshState.observe(viewLifecycleOwner, Observer {
            binding.swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })
        binding.swipeRefresh.setOnRefreshListener {
            adapter.loadInitial(emptyList())
            model.refresh()
        }
    }

    private fun setBottomAppbarClickListeners(){
        //TODO: Delete
        binding.sortButton.setOnClickListener{
            SortSheetDialogFragment(model.sortSelected.value!!) { sort ->
                // TODO: Move logic in ViewModel?
                if (sort == PostSort.TOP || sort == PostSort.CONTROVERSIAL) {
                    TimePeriodSheetDialogFragment { time ->
                        model.loadInitial(model.listingSelected.value!!, sort, time)
                        binding.list.scrollToPosition(0)
                        (binding.list.adapter as? ListingAdapter)?.submitList(null)
                    }.show(parentFragmentManager, TimePeriodSheetDialogFragment.TAG)
                } else {
                    model.loadInitial(model.listingSelected.value!!, sort)
                    binding.list.scrollToPosition(0)
                    (binding.list.adapter as? ListingAdapter)?.submitList(null)
                }

            }.show(parentFragmentManager, SortSheetDialogFragment.TAG)
        }


        binding.subredditButton.setOnClickListener{
            val subredditSelector = SubredditSelectorDialogFragment()
            subredditSelector.setSubredditOnClickListener(object : SubredditOnClickListener {
                override fun onClick(listing: ListingType) {
                    model.loadInitial(listing)
                    subredditSelector.dismiss()
                }
            })
            subredditSelector.show(parentFragmentManager, SubredditSelectorDialogFragment.TAG)
        }

        binding.refreshButton.setOnClickListener{
            adapter.loadInitial(emptyList())
            model.refresh()
        }
    }

    // Post Actions
    override fun vote(post: Post, vote: Vote) {
        model.vote(post.name, vote)
    }

    override fun share(post: Post) {
        TODO("Not yet implemented")
    }

    override fun award(post: Post) {
        TODO("Not yet implemented")
    }

    override fun save(post: Post) {
        if(post.saved) model.unsave(post.name)
        else model.save(post.name)
    }

    override fun hide(post: Post) {
        if(!post.hidden) model.hide(post.name)
        else model.unhide(post.name)
    }

    override fun report(post: Post) {
        TODO("Not yet implemented")
    }

    override fun postClicked(post: Post) {
        parentModel.addReadPost(post.asReadListing())
        viewPagerActions.viewComments(post)
    }

    override fun thumbnailClicked(post: Post) {
        TODO("Not yet implemented")
    }

}
