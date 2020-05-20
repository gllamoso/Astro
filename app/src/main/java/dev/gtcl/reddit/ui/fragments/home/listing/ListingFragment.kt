package dev.gtcl.reddit.ui.fragments.home.listing

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.*
import dev.gtcl.reddit.database.DbMultiReddit
import dev.gtcl.reddit.databinding.FragmentListingBinding
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.ui.*
import dev.gtcl.reddit.ui.activities.main.MainActivity
import dev.gtcl.reddit.ui.activities.main.MainActivityViewModel
import dev.gtcl.reddit.ui.fragments.dialog.ShareOptionsDialogFragment
import dev.gtcl.reddit.ui.fragments.dialog.SortSheetDialogFragment
import dev.gtcl.reddit.database.asAccountDomainModel
import dev.gtcl.reddit.databinding.LayoutNavHeaderBinding
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.activities.main.MainDrawerAdapter
import dev.gtcl.reddit.ui.fragments.ListingScrollListener
import dev.gtcl.reddit.ui.fragments.media.MediaDialogFragment
import dev.gtcl.reddit.ui.fragments.subreddits.SubredditSelectorDialogFragment
import dev.gtcl.reddit.ui.fragments.dialog.TimePeriodSheetDialogFragment

class ListingFragment : Fragment(), PostActions, ListingTypeClickListener, ItemClickListener, SubredditActions, MessageActions {

    private lateinit var binding: FragmentListingBinding

    private val adapter: ListingItemAdapter by lazy {
        ListingItemAdapter(this,
            this,
            this,
            this,
            model::retry,
            true)
    }

    private val scrollChangeListener by lazy{
        ListingScrollListener(loadMore = model::loadAfter)
    }

    private var viewPagerActions: ViewPagerActions? = null
    fun setActions(viewPagerActions: ViewPagerActions){
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
            is SubredditSelectorDialogFragment -> childFragment.setListingTypeClickListener(this)
//            is MediaDialogFragment -> childFragment.postUrlCallback = this::postClicked
        }
    }

    @SuppressLint( "WrongConstant", "RtlHardcoded")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentListingBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model
        setListingInfo()

        //TODO: Add
//        val subredditSelected = savedInstanceState?.getString(KEY_SUBREDDIT) ?: DEFAULT_SUBREDDIT
//        model.getPosts(Subreddit(displayName = subredditSelected))

        // TODO: Update. Wrap with observer, observing a refresh live data
        parentModel.ready.observe(viewLifecycleOwner, Observer{
            if(it == true) {
                model.loadFirstPage()
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

        model.errorMessage.observe(viewLifecycleOwner, Observer {
            if(it != null){
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        })

        setSwipeRefresh()
        setRecyclerView()
        setBottomAppbarClickListeners()
        setLeftDrawer(inflater)
        setRightDrawer()
        return binding.root
    }

    private fun setListingInfo(){
        val args = requireArguments()
        when{
            args.get(DEFAULT_MULTI_REDDIT_KEY) != null -> {
                when(args.get(DEFAULT_MULTI_REDDIT_KEY) as String){
                    FrontPage.toString() -> model.setListingInfo(FrontPage)
                    All.toString() -> model.setListingInfo(All)
                    Popular.toString() -> model.setListingInfo(Popular)
                }
            }
            args.get(MULTI_REDDIT_KEY) != null -> {
                val multiReddit = args.get(MULTI_REDDIT_KEY) as DbMultiReddit
                model.setListingInfo(MultiRedditListing(multiReddit))
            }
            args.get(SUBREDDIT_KEY) != null -> {
                val subreddit = args.get(SUBREDDIT_KEY) as Subreddit
                model.setListingInfo(SubredditListing(subreddit))
            }
            args.get(PROFILE_INFO_KEY) != null -> {
                val profileInfo = args.get(PROFILE_INFO_KEY) as ProfileInfo
                model.setListingInfo(ProfileListing(profileInfo))
            }
        }
    }

    private fun setSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            model.refresh()
        }

        model.refreshState.observe(viewLifecycleOwner, Observer {
            if(it == NetworkState.LOADED){
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }

    private fun setRecyclerView() {
        binding.list.adapter = adapter
        binding.nestedScrollView.setOnScrollChangeListener(scrollChangeListener)
        model.items.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.clearItems()
                adapter.addItems(it)
                scrollChangeListener.finishedLoading()
                if(it.isEmpty()){
                    binding.list.visibility = View.GONE
//                    binding.noResultsText.visibility = View.VISIBLE
                } else {
                    binding.list.visibility = View.VISIBLE
//                    binding.noResultsText.visibility = View.GONE
                }
            }
        })

        model.newItems.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.addItems(it)
                model.newItemsAdded()
                scrollChangeListener.finishedLoading()
            }
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
//            binding.progressBar.visibility = if(it == NetworkState.LOADING) View.VISIBLE else View.GONE
            adapter.networkState = it
        })
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

                    override fun onRemoveAccountClicked(user: String) {
                        parentModel.deleteUserFromDatabase(user)
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
                    sub.userSubscribed = sub.userSubscribed != true
                    binding.rightSideBarLayout.invalidateAll()
                    model.subscribe(sub, if(sub.userSubscribed == true) SubscribeAction.SUBSCRIBE else SubscribeAction.UNSUBSCRIBE)
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

    private fun setBottomAppbarClickListeners(){
        //TODO: Delete
        binding.bottomBarLayout.sortButton.setOnClickListener{
            SortSheetDialogFragment(model.sortSelected.value!!) { sort ->
                // TODO: Move logic in ViewModel?
                if (sort == PostSort.TOP || sort == PostSort.CONTROVERSIAL) {
                    TimePeriodSheetDialogFragment { time ->
                        model.setSort(sort, time)
                        model.loadFirstPage()
                        binding.list.scrollToPosition(0)
                    }
                        .show(childFragmentManager, TimePeriodSheetDialogFragment.TAG)
                } else {
                    model.setSort(sort)
                    model.loadFirstPage()
                    binding.list.scrollToPosition(0)
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

//     _____          _                  _   _
//    |  __ \        | |       /\       | | (_)
//    | |__) |__  ___| |_     /  \   ___| |_ _  ___  _ __  ___
//    |  ___/ _ \/ __| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | (_) \__ \ |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_|   \___/|___/\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

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

//    override fun postClicked(post: Post) {
//        parentModel.addReadPost(post)
//        viewPagerActions.viewComments(post)
//    }

    override fun thumbnailClicked(post: Post) {
        parentModel.addReadPost(post)
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

    override fun onClick(listing: ListingType) {
        model.setListingInfo(listing)
        model.loadFirstPage()
//        adapter.lastItemReached = false
        binding.list.scrollToPosition(0)
//        loadMoreListener.reset()
        for(fragment: Fragment in childFragmentManager.fragments){
            if(fragment is DialogFragment) {
                fragment.dismiss()
            }
        }
    }

    override fun itemClicked(item: Item) {
        viewPagerActions?.navigateToNewPage(item)
    }

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun favorite(subreddit: Subreddit, favorite: Boolean) {
        TODO("Not yet implemented")
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        TODO("Not yet implemented")
    }

//     __  __                                               _   _
//    |  \/  |                                    /\       | | (_)
//    | \  / | ___  ___ ___  __ _  __ _  ___     /  \   ___| |_ _  ___  _ __  ___
//    | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | |  __/\__ \__ \ (_| | (_| |  __/  / ____ \ (__| |_| | (_) | | | \__ \
//    |_|  |_|\___||___/___/\__,_|\__, |\___| /_/    \_\___|\__|_|\___/|_| |_|___/
//                                 __/ |
//                                |___/

    override fun reply(message: Message) {
        TODO("Not yet implemented")
    }

    override fun mark(message: Message) {
        TODO("Not yet implemented")
    }

    override fun delete(message: Message) {
        TODO("Not yet implemented")
    }

    override fun viewProfile(user: String) {
        TODO("Not yet implemented")
    }

    override fun block(user: String) {
        TODO("Not yet implemented")
    }

    companion object{
        fun newInstance(listing: ListingType): ListingFragment {
            val fragment = ListingFragment()
            val args = bundleOf()
            when(listing){
                is FrontPage -> args.putString(DEFAULT_MULTI_REDDIT_KEY, FrontPage.toString())
                is All -> args.putString(DEFAULT_MULTI_REDDIT_KEY, All.toString())
                is Popular -> args.putString(DEFAULT_MULTI_REDDIT_KEY, Popular.toString())
                is MultiRedditListing -> args.putParcelable(MULTI_REDDIT_KEY, listing.multiReddit)
                is SubredditListing -> args.putParcelable(SUBREDDIT_KEY, listing.sub)
                is ProfileListing -> args.putSerializable(PROFILE_INFO_KEY, listing.info)
            }
            fragment.arguments = args
            return fragment
        }
    }

}
