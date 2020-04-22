package dev.gtcl.reddit.ui.fragments.home.listing

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentListingBinding
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.ui.*
import dev.gtcl.reddit.ui.activities.main.MainActivity
import dev.gtcl.reddit.ui.activities.main.MainActivityViewModel
import dev.gtcl.reddit.ui.fragments.dialog.ShareOptionsDialogFragment
import dev.gtcl.reddit.ui.fragments.dialog.SortSheetDialogFragment
import dev.gtcl.reddit.actions.ListingActions
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.actions.ViewPagerActions
import dev.gtcl.reddit.ui.fragments.dialog.imageviewer.MediaDialogFragment
import dev.gtcl.reddit.ui.fragments.dialog.subreddits.SubredditSelectorDialogFragment
import dev.gtcl.reddit.ui.fragments.dialog.TimePeriodSheetDialogFragment

class ListingFragment : Fragment(), PostActions,
    ListingActions {

    private lateinit var binding: FragmentListingBinding
    private lateinit var adapter: dev.gtcl.reddit.ui.ListingAdapter

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

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is SubredditSelectorDialogFragment -> childFragment.setListingActions(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(!::binding.isInitialized)
            setupFragment(inflater)
        return binding.root
    }

    private fun setupFragment(inflater: LayoutInflater){
        binding = FragmentListingBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model

        //TODO: Add
//        val subredditSelected = savedInstanceState?.getString(KEY_SUBREDDIT) ?: DEFAULT_SUBREDDIT
//        model.getPosts(Subreddit(displayName = subredditSelected))

        // TODO: Update. Wrap with observer, observing a refresh live data
        parentModel.fetchData.observe(viewLifecycleOwner, Observer{
            if(it) { model.loadInitial(FrontPage) }
        })

        binding.toolbar.setNavigationOnClickListener {
            parentModel.openDrawer()
        }

        setRecyclerView()
        setBottomAppbarClickListeners()
    }

    private fun setRecyclerView() {
        val loadMoreScrollListener = LoadMoreScrollListener(
            binding.list.layoutManager as GridLayoutManager,
            object : OnLoadMoreListener {
                override fun loadMore() {
                    model.loadAfter()
                }
            })

        adapter = ListingAdapter(this as PostActions, { model.retry()}, {loadMoreScrollListener.finishedLoading()})

        binding.list.adapter = adapter
        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })

        parentModel.allReadPosts.observe(viewLifecycleOwner, Observer {
            adapter.setReadSubs(it)
        })

        model.initialListing.observe(viewLifecycleOwner, Observer {
            if(it != null) {
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

    private fun setBottomAppbarClickListeners(){
        //TODO: Delete
        binding.sortButton.setOnClickListener{
            SortSheetDialogFragment(model.sortSelected.value!!) { sort ->
                // TODO: Move logic in ViewModel?
                if (sort == PostSort.TOP || sort == PostSort.CONTROVERSIAL) {
                    TimePeriodSheetDialogFragment { time ->
                        model.loadInitial(model.listingSelected.value!!, sort, time)
                        binding.list.scrollToPosition(0)
                        (binding.list.adapter as? ListingAdapter)?.loadInitial(listOf())
                    }
                        .show(childFragmentManager, TimePeriodSheetDialogFragment.TAG)
                } else {
                    model.loadInitial(model.listingSelected.value!!, sort)
                    binding.list.scrollToPosition(0)
                    (binding.list.adapter as? ListingAdapter)?.loadInitial(listOf())
                }

            }
                .show(childFragmentManager, SortSheetDialogFragment.TAG)
        }


        binding.subredditButton.setOnClickListener{
            val subredditSelector = SubredditSelectorDialogFragment()
            subredditSelector.show(childFragmentManager, SubredditSelectorDialogFragment.TAG)
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
        val args = Bundle()
        args.putParcelable(POST_KEY, post)
        val shareOptionsFragment = ShareOptionsDialogFragment()
        shareOptionsFragment.arguments = args
        shareOptionsFragment.show(childFragmentManager, null)
    }

    override fun viewProfile(post: Post) {
        val bundle = bundleOf(USER_KEY to post.author)
        findNavController().navigate(R.id.account_fragment, bundle)
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
        parentModel.addReadPost(post.asReadListing)
        viewPagerActions.viewComments(post)
    }

    override fun thumbnailClicked(post: Post) {
        Log.d("TAE","Post clicked: $post")
        val urlType = when {
            post.isImage -> UrlType.IMAGE
            post.isGif -> UrlType.GIF
            post.isGfycat -> UrlType.GFYCAT
            post.isGfv -> UrlType.GIFV
            post.isRedditVideo -> UrlType.M3U8
            else -> UrlType.LINK
        }
        Log.d("TAE", "UrlType: $urlType")
        val dialog = MediaDialogFragment.newInstance(
            if(urlType == UrlType.M3U8 || urlType == UrlType.GIFV) post.videoUrl!! else post.url!!,
            urlType,
            post.permalink,
            if(urlType == UrlType.GFYCAT) post.videoUrl else null)
        dialog.show(childFragmentManager, null)
    }

    override fun onClick(listing: ListingType) {
        model.loadInitial(listing)
        for(fragment: Fragment in childFragmentManager.fragments){
            if(fragment is DialogFragment) fragment.dismiss()
        }
    }

}
