package dev.gtcl.reddit.ui.fragments.home.listing

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.LeftDrawerActions
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
import dev.gtcl.reddit.database.asAccountDomainModel
import dev.gtcl.reddit.databinding.LayoutNavHeaderBinding
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.activities.main.MainDrawerAdapter
import dev.gtcl.reddit.ui.fragments.dialog.media.MediaDialogFragment
import dev.gtcl.reddit.ui.fragments.dialog.subreddits.SubredditSelectorDialogFragment
import dev.gtcl.reddit.ui.fragments.dialog.TimePeriodSheetDialogFragment

class ListingFragment : Fragment(), PostActions, ListingActions {

    private lateinit var binding: FragmentListingBinding
    private lateinit var adapter: ListingAdapter
    private lateinit var loadMoreListener: LoadMoreScrollListener

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
            is SubredditSelectorDialogFragment -> childFragment.listingActions = this
            is MediaDialogFragment -> childFragment.postUrlCallback = this::postClicked
        }
    }

    @SuppressLint( "WrongConstant", "RtlHardcoded")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentListingBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model

        //TODO: Add
//        val subredditSelected = savedInstanceState?.getString(KEY_SUBREDDIT) ?: DEFAULT_SUBREDDIT
//        model.getPosts(Subreddit(displayName = subredditSelected))

        // TODO: Update. Wrap with observer, observing a refresh live data
        parentModel.ready.observe(viewLifecycleOwner, Observer{
            if(it == true) {
                model.loadInitial(FrontPage)
                parentModel.readyComplete()
            }
        })

        model.listingSelected.observe(viewLifecycleOwner, Observer {
            if(it != null && it is SubredditListing){
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT)
            } else {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT)
            }
        })

        setRecyclerView()
        setBottomAppbarClickListeners()
        setLeftDrawer(inflater)
        setRightDrawer()
        return binding.root
    }

    @SuppressLint("RtlHardcoded")
    private fun setLeftDrawer(inflater: LayoutInflater){
        val header = LayoutNavHeaderBinding.inflate(inflater)
        binding.expandableListView.addHeaderView(header.root)
        val adapter = MainDrawerAdapter(requireContext(),
            object :
                LeftDrawerActions {
                    override fun onAddAccountClicked() {
                        parentModel.startSignInActivity()
                    }

                    override fun onRemoveAccountClicked(username: String) {
                        parentModel.deleteUserFromDatabase(username)
                    }

                    override fun onAccountClicked(account: Account) {
                        parentModel.setCurrentUser(account, true)
                        binding.drawerLayout.closeDrawer(Gravity.LEFT)
                    }

                    override fun onLogoutClicked() {
                        parentModel.setCurrentUser(null, true)
                        binding.drawerLayout.closeDrawer(Gravity.LEFT)
                    }

                    override fun onHomeClicked() {
                        findNavController().popBackStack(R.id.home_fragment, false)
                        binding.drawerLayout.closeDrawer(Gravity.LEFT)
                    }

                    override fun onMyAccountClicked() {
                        findNavController().navigate(R.id.account_fragment)
                        binding.drawerLayout.closeDrawer(Gravity.LEFT)
                    }

                    override fun onInboxClicked() {
                        if((activity?.application as RedditApplication).accessToken == null){
                            Snackbar.make(binding.drawerLayout, R.string.please_login_error, Snackbar.LENGTH_SHORT).show()
                        } else {
                            findNavController().navigate(R.id.messages_fragment)
                            binding.drawerLayout.closeDrawer(Gravity.LEFT)
                        }
                    }

                    override fun onSettingsClicked() {
                        Toast.makeText(context, "Settings", Toast.LENGTH_LONG).show()
                        binding.drawerLayout.closeDrawer(Gravity.LEFT)
                    }

            })

        binding.expandableListView.setAdapter(adapter)

        parentModel.allUsers.observe(viewLifecycleOwner, Observer {
            adapter.setUsers(it.asAccountDomainModel())
        })

        parentModel.currentAccount.observe(viewLifecycleOwner, Observer {
            header.account = it
        })

        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        binding.topAppBar.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.LEFT)
        }
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener{
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerClosed(drawerView: View) {
                binding.expandableListView.collapseGroup(0)
            }

            override fun onDrawerOpened(drawerView: View) {
                adapter.notifyDataSetInvalidated()
            }
        })
    }

    @SuppressLint("RtlHardcoded")
    private fun setRightDrawer(){
        binding.topAppBar.sideBarButton.setOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.RIGHT)
        }

        model.subredditSelected.observe(viewLifecycleOwner, Observer {sub ->
            if(sub != null){
                binding.rightSideBarLayout.addIcon.setOnClickListener {
                    sub.isAddedToDb = !sub.isAddedToDb
                    binding.rightSideBarLayout.invalidateAll()
                    model.subscribe(sub, if(sub.isAddedToDb) SubscribeAction.SUBSCRIBE else SubscribeAction.UNSUBSCRIBE)
                }
                binding.rightSideBarLayout.favoriteIcon.setOnClickListener {
                    sub.isFavorite = !sub.isFavorite
                    binding.rightSideBarLayout.invalidateAll()
                    model.addToFavorites(sub, sub.isFavorite)
                }
            } else {
                binding.rightSideBarLayout.addIcon.isClickable = false
                binding.rightSideBarLayout.favoriteIcon.isClickable = false
            }
            binding.rightSideBarLayout.invalidateAll()
        })

        childFragmentManager.setFragmentResultListener(SUBREDDIT_UPDATE_REQUEST_KEY, viewLifecycleOwner, FragmentResultListener{ _, bundle ->
            val subName = bundle.get(STRING_KEY) as String
            if(subName == model.subredditSelected.value?.displayName){
                model.syncSubreddit()
            }
        })
    }

    private fun setRecyclerView() {
        loadMoreListener = LoadMoreScrollListener(
            binding.list.layoutManager as GridLayoutManager
        ) {model.loadAfter()}

        adapter = ListingAdapter(postActions = this as PostActions,
            retry = model::retry,
            onLastItemReached = loadMoreListener::lastItemReached)

        binding.list.adapter = adapter
        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })

        parentModel.allReadPosts.observe(viewLifecycleOwner, Observer {
            adapter.setReadSubs(it)
        })

        model.initialListing.observe(viewLifecycleOwner, Observer {
            if(it == null) {
                return@Observer
            }
            adapter.loadInitial(it)
            model.loadInitialFinished()
        })

        binding.list.addOnScrollListener(loadMoreListener)

        model.additionalListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.loadMore(it)
                model.loadAfterFinished()
                loadMoreListener.finishedLoading()
            }
        })

        (binding.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        binding.swipeRefresh.setOnRefreshListener {
            model.refresh()
        }

        model.refreshState.observe(viewLifecycleOwner, Observer {
            binding.swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })
    }

    private fun setBottomAppbarClickListeners(){
        //TODO: Delete
        binding.bottomBarLayout.sortButton.setOnClickListener{
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


        binding.bottomBarLayout.subredditButton.setOnClickListener{
            val subredditSelector = SubredditSelectorDialogFragment()
            subredditSelector.show(childFragmentManager, SubredditSelectorDialogFragment.TAG)
        }

        binding.bottomBarLayout.refreshButton.setOnClickListener{
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
        parentModel.addReadPost(post.asReadListing)
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
            post)
        dialog.show(childFragmentManager, null)
    }

    override fun onListingClicked(listing: ListingType) {
        model.loadInitial(listing)
        adapter.lastItemReached = false
        binding.list.scrollToPosition(0)
        loadMoreListener.reset()
        for(fragment: Fragment in childFragmentManager.fragments){
            if(fragment is DialogFragment) {
                fragment.dismiss()
            }
        }
    }

}
