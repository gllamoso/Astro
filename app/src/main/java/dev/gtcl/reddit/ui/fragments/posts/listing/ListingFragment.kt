package dev.gtcl.reddit.ui.fragments.posts.listing

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.Vote
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
            if(it) { model.fetchPosts(FrontPage) }
        })

        binding.toolbar.setNavigationOnClickListener {
            parentModel.openDrawer()
        }

        setRecyclerView()
        setSwipeToRefresh()
        setBottomAppbarClickListeners()
    }

//    private val postViewClickListener = object :
//        ListingItemClickListener {
//        override fun onPostClicked(post: Post?, position: Int) {
//            post?.let {
//                model.addReadPost(it.asReadListing())
//                postClickListener(it)
//            }
//        }
//
//        override fun onThumbnailClicked(post: Post) {
//            val dialogFragment = ImageVideoViewerDialogFragment()
//            dialogFragment.setPost(post)
//            dialogFragment.show(parentFragmentManager, "test")
//        }
//
//    }

    private fun setRecyclerView() {
        val adapter = ListingAdapter({model.retry()}, this)

        binding.list.adapter = adapter
        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })

        parentModel.allReadPosts.observe(viewLifecycleOwner, Observer {
            adapter.setReadSubs(it)
        })
    }

    private fun setSwipeToRefresh() {
        model.refreshState.observe(viewLifecycleOwner, Observer {
            binding.swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })
        binding.swipeRefresh.setOnRefreshListener {
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
                        model.fetchPosts(model.listingSelected.value!!, sort, time)
                        binding.list.scrollToPosition(0)
                        (binding.list.adapter as? ListingAdapter)?.submitList(null)
                    }.show(parentFragmentManager, TimePeriodSheetDialogFragment.TAG)
                } else {
                    model.fetchPosts(model.listingSelected.value!!, sort)
                    binding.list.scrollToPosition(0)
                    (binding.list.adapter as? ListingAdapter)?.submitList(null)
                }

            }.show(parentFragmentManager, SortSheetDialogFragment.TAG)
        }


        binding.subredditButton.setOnClickListener{
            val subredditSelector = SubredditSelectorDialogFragment()
            subredditSelector.setSubredditOnClickListener(object : SubredditOnClickListener {
                override fun onClick(listing: ListingType) {
                    model.fetchPosts(listing)
                    subredditSelector.dismiss()
                }
            })
            subredditSelector.show(parentFragmentManager, SubredditSelectorDialogFragment.TAG)
        }

        binding.refreshButton.setOnClickListener{
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
        TODO("Not yet implemented")
    }

    override fun report(post: Post) {
        TODO("Not yet implemented")
    }

    override fun postClicked(post: Post) {
        viewPagerActions.viewComments(post)
    }

    override fun thumbnailClicked(post: Post) {
        TODO("Not yet implemented")
    }

}
