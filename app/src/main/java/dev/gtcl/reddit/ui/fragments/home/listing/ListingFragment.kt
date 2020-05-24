package dev.gtcl.reddit.ui.fragments.home.listing

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
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
import androidx.recyclerview.widget.GridLayoutManager
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
        ItemScrollListener(15, binding.list.layoutManager as GridLayoutManager, model::loadAfter)
    }

    private var viewPagerActions: ViewPagerActions? = null
    fun setActions(viewPagerActions: ViewPagerActions){
        this.viewPagerActions = viewPagerActions
    }

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }

    private val model: ListingViewModel by lazy {
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

        // TODO: Update. Wrap with observer, observing a refresh live data
        parentModel.ready.observe(viewLifecycleOwner, Observer{
            if(it == true) {
                model.loadFirstPage()
                parentModel.readyComplete()
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
        val listingType = requireArguments().getParcelable(LISTING_KEY) as ListingType
        model.setListingInfo(listingType)
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
        binding.list.addOnScrollListener(scrollChangeListener)
        model.items.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.clearItems()
                adapter.addItems(it)
                scrollChangeListener.finishedLoading()
                if(it.isEmpty()){
                    binding.list.visibility = View.GONE
                    binding.noResultsText.visibility = View.VISIBLE
                } else {
                    binding.list.visibility = View.VISIBLE
                    binding.noResultsText.visibility = View.GONE
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
                binding.rightSideBarLayout.addButton.setOnClickListener {
                    sub.userSubscribed = sub.userSubscribed != true
                    binding.rightSideBarLayout.invalidateAll()
                    model.subscribe(sub, if(sub.userSubscribed == true) SubscribeAction.SUBSCRIBE else SubscribeAction.UNSUBSCRIBE)
                }
                binding.rightSideBarLayout.favoriteButton.setOnClickListener {
                    sub.isFavorite = !sub.isFavorite
                    binding.rightSideBarLayout.invalidateAll()
                    model.addToFavorites(sub, sub.isFavorite)
                }
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT)
            } else {
                binding.rightSideBarLayout.addButton.isClickable = false
                binding.rightSideBarLayout.favoriteButton.isClickable = false
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT)
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
        adapter.clearItems()
        binding.list.scrollToPosition(0)
        scrollChangeListener.reset()
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

    override fun favorite(subreddit: Subreddit, favorite: Boolean) {}

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {}

//     __  __                                               _   _
//    |  \/  |                                    /\       | | (_)
//    | \  / | ___  ___ ___  __ _  __ _  ___     /  \   ___| |_ _  ___  _ __  ___
//    | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | |  __/\__ \__ \ (_| | (_| |  __/  / ____ \ (__| |_| | (_) | | | \__ \
//    |_|  |_|\___||___/___/\__,_|\__, |\___| /_/    \_\___|\__|_|\___/|_| |_|___/
//                                 __/ |
//                                |___/

    override fun reply(message: Message) {}

    override fun mark(message: Message) {}

    override fun delete(message: Message) {}

    override fun viewProfile(user: String) {}

    override fun block(user: String) {}

    companion object{
        fun newInstance(listing: ListingType): ListingFragment {
            return ListingFragment().apply {
                arguments = bundleOf(LISTING_KEY to listing)
            }
        }
    }

}
