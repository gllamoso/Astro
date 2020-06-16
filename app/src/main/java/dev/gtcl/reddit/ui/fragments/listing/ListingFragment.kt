package dev.gtcl.reddit.ui.fragments.listing

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.*
import dev.gtcl.reddit.database.SavedAccount
import dev.gtcl.reddit.databinding.FragmentListingBinding
import dev.gtcl.reddit.ui.*
import dev.gtcl.reddit.databinding.LayoutNavHeaderBinding
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.activities.MainDrawerAdapter
import dev.gtcl.reddit.ui.fragments.AccountPage
import dev.gtcl.reddit.ui.fragments.ViewPagerFragmentDirections
import dev.gtcl.reddit.ui.fragments.media.MediaDialogFragment
import dev.gtcl.reddit.ui.fragments.misc.ShareOptionsDialogFragment
import dev.gtcl.reddit.ui.fragments.misc.SortDialogFragment
import dev.gtcl.reddit.ui.fragments.misc.TimeDialogFragment
import dev.gtcl.reddit.ui.fragments.subreddits.SubscriptionsDialogFragment

class ListingFragment : Fragment(), PostActions, SubredditActions, ListingTypeClickListener,
    ItemClickListener, LeftDrawerActions, SortActions {

    private lateinit var binding: FragmentListingBinding
    private lateinit var scrollListener: ItemScrollListener
    private var viewPagerActions: ViewPagerActions? = null
    private var navigationActions: NavigationActions? = null

    private val model: ListingVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ListingVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    fun setActions(
        viewPagerActions: ViewPagerActions,
        navigationActions: NavigationActions
    ) {
        this.viewPagerActions = viewPagerActions
        this.navigationActions = navigationActions
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when (childFragment) {
            is SortDialogFragment -> childFragment.setActions(this)
            is TimeDialogFragment -> childFragment.setActions(this)
            is SubscriptionsDialogFragment -> childFragment.setActions(this, this)
        }
    }

    override fun onResume() {
        super.onResume()
        model.syncSubreddit()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListingBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model
        setListingInfo()

        if (!model.initialPageLoaded) {
            model.loadMore()
        }

        setSwipeRefresh()
        setRecyclerView()
        setBottomAppbarClickListeners()
        setLeftDrawer(inflater)
        setRightDrawer()
        setOtherObservers()

        return binding.root
    }

    private fun setListingInfo() {
        val listingType = requireArguments().getParcelable(LISTING_KEY) as ListingType
        model.setListingInfo(listingType)
    }

    private fun setSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            model.refresh()
        }

        model.refreshState.observe(viewLifecycleOwner, Observer {
            if (it == NetworkState.LOADED) {
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }

    private fun setRecyclerView() {
        val adapter = ListingItemAdapter(
            postActions = this,
            itemClickListener = this,
            retry = model::retry
        )

        binding.list.adapter = adapter
        scrollListener = ItemScrollListener(
            15,
            binding.list.layoutManager as GridLayoutManager,
            model::loadMore
        )
        binding.list.addOnScrollListener(scrollListener)

        model.items.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                val prevCount = adapter.itemCount
                adapter.setItems(it)
                if(prevCount >= it.size){
                    binding.list.scrollToPosition(0)
                }
                scrollListener.finishedLoading()
                if (it.isEmpty()) {
                    binding.list.visibility = View.GONE
                    binding.noResultsText.visibility = View.VISIBLE
                } else {
                    binding.list.visibility = View.VISIBLE
                    binding.noResultsText.visibility = View.GONE
                }
            }
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.networkState = it
        })

        model.lastItemReached.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                binding.list.removeOnScrollListener(scrollListener)
            }
        })
    }

    @SuppressLint("RtlHardcoded")
    private fun setLeftDrawer(inflater: LayoutInflater) {
        val header = LayoutNavHeaderBinding.inflate(inflater)
        binding.expandableListView.addHeaderView(header.root)
        val adapter = MainDrawerAdapter(
            requireContext(),
            this
        )

        binding.expandableListView.setAdapter(adapter)

        activityModel.allUsers.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.setUsers(it)
            }
        })

        header.account = (requireActivity().application as RedditApplication).currentAccount

        binding.topAppBar.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.LEFT)
        }

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
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
    private fun setRightDrawer() {
        binding.topAppBar.sideBarButton.setOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.RIGHT)
        }

        binding.rightSideBarLayout.lifecycleOwner = this
        model.subreddit.observe(viewLifecycleOwner, Observer { sub ->
            if (sub != null) {
                binding.rightSideBarLayout.addButton.setOnClickListener {
                    sub.userSubscribed = sub.userSubscribed != true
                    subscribe(sub, (sub.userSubscribed == true))
                }
                binding.drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    Gravity.RIGHT
                )
            } else {
                binding.rightSideBarLayout.addButton.isClickable = false
                binding.drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    Gravity.RIGHT
                )
            }
        })
    }

    private fun setBottomAppbarClickListeners() {

        binding.bottomBarLayout.sortButton.setOnClickListener {
            SortDialogFragment.newInstance(model.postSort.value!!, model.time.value)
                .show(childFragmentManager, null)
        }

        binding.bottomBarLayout.subredditButton.setOnClickListener {
            SubscriptionsDialogFragment().show(childFragmentManager, null)
        }

        binding.bottomBarLayout.refreshButton.setOnClickListener {
            model.refresh()
        }
    }

    private fun setOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        })
    }

//     _____          _                  _   _
//    |  __ \        | |       /\       | | (_)
//    | |__) |__  ___| |_     /  \   ___| |_ _  ___  _ __  ___
//    |  ___/ _ \/ __| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | (_) \__ \ |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_|   \___/|___/\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun vote(post: Post, vote: Vote) {
        activityModel.vote(post.name, vote)
    }

    override fun share(post: Post) {
        ShareOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
    }

    override fun viewProfile(post: Post) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(post.author)
            ))
    }

    override fun save(post: Post) {
        activityModel.save(post.name, post.saved)
    }

    override fun hide(post: Post, position: Int) {
        activityModel.hide(post.name, post.hidden)
        if(post.hidden){
            model.removeItemAt(position)
        }
    }

    override fun report(post: Post) {
        TODO("Implement reporting")
    }

    override fun thumbnailClicked(post: Post) {
        model.addReadItem(post)
        val urlType = when {
            post.isImage -> UrlType.IMAGE
            post.isGif -> UrlType.GIF
            post.isGfycat -> UrlType.GFYCAT
            post.isGfv -> UrlType.GIFV
            post.isRedditVideo -> UrlType.M3U8
            else -> UrlType.LINK
        }
        if(urlType == UrlType.LINK){
            navigationActions?.launchWebview(post.url!!)
        } else {
            val dialog = MediaDialogFragment.newInstance(
                if(urlType == UrlType.M3U8 || urlType == UrlType.GIFV) post.previewVideoUrl!! else post.url!!,
                urlType,
                post)
            dialog.show(parentFragmentManager, null)
        }
    }

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
//        parentSubredditActions?.subscribe(subreddit, subscribe)
    }

//     _      _     _   _               _______                  _____ _ _      _      _      _     _
//    | |    (_)   | | (_)             |__   __|                / ____| (_)    | |    | |    (_)   | |
//    | |     _ ___| |_ _ _ __   __ _     | |_   _ _ __   ___  | |    | |_  ___| | __ | |     _ ___| |_ ___ _ __   ___ _ __
//    | |    | / __| __| | '_ \ / _` |    | | | | | '_ \ / _ \ | |    | | |/ __| |/ / | |    | / __| __/ _ \ '_ \ / _ \ '__|
//    | |____| \__ \ |_| | | | | (_| |    | | |_| | |_) |  __/ | |____| | | (__|   <  | |____| \__ \ ||  __/ | | |  __/ |
//    |______|_|___/\__|_|_| |_|\__, |    |_|\__, | .__/ \___|  \_____|_|_|\___|_|\_\ |______|_|___/\__\___|_| |_|\___|_|
//                               __/ |        __/ | |
//                              |___/        |___/|_|

    override fun listingTypeClicked(listing: ListingType) {
        navigationActions?.listingSelected(listing)
        for (fragment: Fragment in childFragmentManager.fragments) {
            if (fragment is DialogFragment) {
                fragment.dismiss()
            }
        }
    }

//     _____ _                    _____ _ _      _      _      _     _
//    |_   _| |                  / ____| (_)    | |    | |    (_)   | |
//      | | | |_ ___ _ __ ___   | |    | |_  ___| | __ | |     _ ___| |_ ___ _ __   ___ _ __
//      | | | __/ _ \ '_ ` _ \  | |    | | |/ __| |/ / | |    | / __| __/ _ \ '_ \ / _ \ '__|
//     _| |_| ||  __/ | | | | | | |____| | | (__|   <  | |____| \__ \ ||  __/ | | |  __/ |
//    |_____|\__\___|_| |_| |_|  \_____|_|_|\___|_|\_\ |______|_|___/\__\___|_| |_|\___|_|

    override fun itemClicked(item: Item) {
        viewPagerActions?.navigateToNewPage(item)
    }

//     _           __ _     _____                                             _   _
//    | |         / _| |   |  __ \                                  /\       | | (_)
//    | |     ___| |_| |_  | |  | |_ __ __ ___      _____ _ __     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \  _| __| | |  | | '__/ _` \ \ /\ / / _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___|  __/ | | |_  | |__| | | | (_| |\ V  V /  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//    |______\___|_|  \__| |_____/|_|  \__,_| \_/\_/ \___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/

    @SuppressLint("RtlHardcoded")
    override fun onAddAccountClicked() {
        navigationActions?.signInNewAccount()
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    override fun onRemoveAccountClicked(user: String) {
//                        parentModel.deleteUserFromDatabase(user)
    }

    @SuppressLint("RtlHardcoded")
    override fun onAccountClicked(account: SavedAccount) {
//                        parentModel.setCurrentUser(account, true)
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onLogoutClicked() {
//                        parentModel.setCurrentUser(null, true)
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onHomeClicked() {
//                        findNavController().popBackStack(R.id.home_fragment, false)
//        navigationActions?.homeClicked()
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onMyAccountClicked() {
        if ((activity?.application as RedditApplication).accessToken == null) {
            Snackbar.make(binding.drawerLayout, R.string.please_login, Snackbar.LENGTH_SHORT)
                .show()
        } else {
            navigationActions?.accountSelected(null)
            binding.drawerLayout.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onInboxClicked() {
        if ((activity?.application as RedditApplication).accessToken == null) {
            Snackbar.make(binding.drawerLayout, R.string.please_login, Snackbar.LENGTH_SHORT)
                .show()
        } else {
            navigationActions?.messagesSelected()
            binding.drawerLayout.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onSettingsClicked() {
        Toast.makeText(context, "Settings", Toast.LENGTH_LONG).show()
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

//      _____            _                  _   _
//     / ____|          | |       /\       | | (_)
//    | (___   ___  _ __| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \ / _ \| '__| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | (_) | |  | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \___/|_|   \__| /_/    \_\___|\__|_|\___/|_| |_|___/

    override fun sortSelected(sort: PostSort, time: Time?) {
        model.setSort(sort, time)
        model.refresh()
    }

    companion object {
        fun newInstance(listing: ListingType): ListingFragment {
            return ListingFragment().apply {
                arguments = bundleOf(LISTING_KEY to listing)
            }
        }
    }

}
