package dev.gtcl.reddit.ui.fragments.listing

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.*
import dev.gtcl.reddit.database.SavedAccount
import dev.gtcl.reddit.databinding.FragmentListingBinding
import dev.gtcl.reddit.ui.*
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.network.Status
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.*
import dev.gtcl.reddit.ui.fragments.create_post.CreatePostDialogFragment
import dev.gtcl.reddit.ui.fragments.media.MediaDialogFragment
import dev.gtcl.reddit.ui.fragments.misc.ShareOptionsDialogFragment
import dev.gtcl.reddit.ui.fragments.misc.SortDialogFragment
import dev.gtcl.reddit.ui.fragments.misc.TimeDialogFragment
import dev.gtcl.reddit.ui.fragments.subreddits.SubscriptionsDialogFragment
import io.noties.markwon.Markwon

class ListingFragment : Fragment(), PostActions, CommentActions, SubredditActions,
    ItemClickListener, LeftDrawerActions, SortActions, LinkHandler {

    private lateinit var binding: FragmentListingBinding

    private val model: ListingVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ListingVM::class.java)
    }

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private val markwon: Markwon by lazy {
        createMarkwonInstance(requireContext(), ::handleLink)
    }

    private lateinit var scrollListener: ItemScrollListener
    private lateinit var listAdapter: ListingItemAdapter

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when (childFragment) {
            is SortDialogFragment -> childFragment.setActions(this)
            is TimeDialogFragment -> childFragment.setActions(this)
        }
    }

    override fun onResume() {
        super.onResume()
        model.setLeftDrawerExpanded(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListingBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.model = model

        if (!model.firstPageLoaded) {
            initData()
        }
        initScroller()
        initBottomBar()
        initLeftDrawer()
        initRightDrawer()
        initOtherObservers()

        return binding.root
    }

    private fun initData(){
        val listing = requireArguments().getParcelable(LISTING_KEY) as Listing
        model.setListing(listing)
        when(listing){
            is SubredditListing -> model.fetchSubreddit(listing.displayName)
            is SubscriptionListing -> {
                if(listing.subscription.type == SubscriptionType.SUBREDDIT){
                    model.fetchSubreddit(listing.subscription.displayName)
                }
            }
        }
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val showNsfw = sharedPref.getBoolean("nsfw", true)
        model.showNsfw = showNsfw
        model.fetchFirstPage()
    }

    private fun initScroller() {
        scrollListener = ItemScrollListener(15, binding.list.layoutManager as GridLayoutManager, model::loadMore)
        listAdapter = ListingItemAdapter(markwon, postActions = this, commentActions = this, expected = ItemType.Post, itemClickListener = this, retry = model::retry)
        binding.list.apply {
            this.adapter = listAdapter
            addOnScrollListener(scrollListener)
        }

        model.items.observe(viewLifecycleOwner, Observer {
            scrollListener.finishedLoading()
            listAdapter.submitList(it)
        })

        model.moreItems.observe(viewLifecycleOwner, Observer {
            if(it != null){
                scrollListener.finishedLoading()
                listAdapter.addItems(it)
                model.moreItemsObserved()
            }
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
            listAdapter.networkState = it
            if (it == NetworkState.LOADED || it.status == Status.FAILED) {
                binding.swipeRefresh.isRefreshing = false
            }
        })

        model.lastItemReached.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                binding.list.removeOnScrollListener(scrollListener)
            }
        })
    }

    @SuppressLint("RtlHardcoded")
    private fun initLeftDrawer() {
        val leftDrawerAdapter = LeftDrawerAdapter(requireContext(), this, LeftDrawerHeader.HOME)
        binding.leftDrawerLayout.list.adapter = leftDrawerAdapter
        binding.leftDrawerLayout.account = (requireActivity().application as RedditApplication).currentAccount

        activityModel.allUsers.observe(viewLifecycleOwner, Observer {
            leftDrawerAdapter.submitUsers(it)
        })

        model.leftDrawerExpanded.observe(viewLifecycleOwner, Observer {
            leftDrawerAdapter.isExpanded = it
        })

        binding.leftDrawerLayout.banner.setOnClickListener {
            model.toggleLeftDrawerExpanding()
        }

        binding.topAppBar.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.LEFT)
        }

        model.leftDrawerExpanded.observe(viewLifecycleOwner, Observer {
            rotateView(binding.leftDrawerLayout.expandedIndicator, it)
        })
    }

    @SuppressLint("RtlHardcoded")
    private fun initRightDrawer() {
        binding.topAppBar.sideBarButton.setOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.RIGHT)
        }

        binding.rightDrawerLayout.lifecycleOwner = this
        model.subreddit.observe(viewLifecycleOwner, Observer { sub ->
            if (sub != null) {
                binding.rightDrawerLayout.addButton.setOnClickListener {
                    sub.userSubscribed = sub.userSubscribed != true
                    subscribe(sub, (sub.userSubscribed == true))
                }
                binding.drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    Gravity.RIGHT
                )
            } else {
                binding.rightDrawerLayout.addButton.isClickable = false
                binding.drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    Gravity.RIGHT
                )
            }
        })
    }

    private fun initBottomBar() {

        binding.bottomBarLayout.sortButton.setOnClickListener {
            SortDialogFragment.newInstance(model.postSort.value!!, model.time.value)
                .show(childFragmentManager, null)
        }

        binding.bottomBarLayout.subredditButton.setOnClickListener {
            SubscriptionsDialogFragment().show(childFragmentManager, null)
        }

        binding.bottomBarLayout.refreshButton.setOnClickListener {
            initData()
        }

        binding.bottomBarLayout.moreOptionsButton.setOnClickListener {
            val popupMenu = PopupMenu(context, it)
            popupMenu.inflate(R.menu.listing_more_menu)
            popupMenu.forceIcons()
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.post -> {
                        val subredditName = model.subreddit.value?.displayName
                        CreatePostDialogFragment.newInstance(subredditName).show(parentFragmentManager, null)
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    private fun initOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            initData()
        }

        childFragmentManager.setFragmentResultListener(LISTING_KEY, viewLifecycleOwner){ _, bundle ->
            val listing = bundle.get(LISTING_KEY) as Listing
            if(listing is SubscriptionListing && listing.subscription.type == SubscriptionType.USER){
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(listing.subscription.displayName)))
            } else {
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(ListingPage(listing)))
            }
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
        when(vote){
            Vote.UPVOTE -> {
                when(post.likes){
                    true -> post.score--
                    false -> post.score += 2
                    null -> post.score++
                }
            }
            Vote.DOWNVOTE -> {
                when(post.likes){
                    true -> post.score -= 2
                    false -> post.score ++
                    null -> post.score--
                }
            }
            Vote.UNVOTE -> {
                when(post.likes){
                    true -> post.score--
                    false -> post.score++
                }
            }
        }
        activityModel.vote(post.name, vote)
    }

    override fun share(post: Post) {
        ShareOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
    }

    override fun viewProfile(post: Post) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(post.author)
            )
        )
    }

    override fun save(post: Post) {
        activityModel.save(post.name, post.saved)
    }

    override fun subredditSelected(sub: String) {
        model.listing.let {
            if(it is SubredditListing && it.displayName == sub){
                return
            }
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(ListingPage(SubredditListing(sub))))
        }
    }

    override fun hide(post: Post, position: Int) {
        activityModel.hide(post.name, post.hidden)
        model.removeItemAt(position)
        listAdapter.removeAt(position)
    }

    override fun report(post: Post) {
        TODO("Implement reporting")
    }

    override fun thumbnailClicked(post: Post, position: Int) {
        model.addReadItem(post)
        when (val urlType: UrlType? = post.url?.getUrlType()) {
            UrlType.OTHER -> activityModel.openChromeTab(post.url)
            null -> throw IllegalArgumentException("Post does not have URL")
            else -> {
                val mediaType = when (urlType) {
                    UrlType.IMGUR_ALBUM -> MediaType.IMGUR_ALBUM
                    UrlType.GIF -> MediaType.GIF
                    UrlType.GFYCAT -> MediaType.GFYCAT
                    UrlType.IMAGE -> MediaType.PICTURE
                    UrlType.HLS, UrlType.GIFV, UrlType.STANDARD_VIDEO, UrlType.REDDIT_VIDEO -> MediaType.VIDEO
                    else -> throw IllegalArgumentException("Invalid media type: $urlType")
                }
                val url = when (mediaType) {
                    MediaType.VIDEO -> post.previewVideoUrl!!
                    else -> post.url
                }
                val backupUrl = when (mediaType) {
                    MediaType.GFYCAT -> post.previewVideoUrl
                    else -> null
                }
                val dialog = MediaDialogFragment.newInstance(
                    MediaURL(url, mediaType, backupUrl),
                    PostPage(post, position)
                )
                dialog.show(parentFragmentManager, null)
            }
        }
    }

//      _____                                     _                  _   _
//     / ____|                                   | |       /\       | | (_)
//    | |     ___  _ __ ___  _ __ ___   ___ _ __ | |_     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___| (_) | | | | | | | | | | |  __/ | | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//     \_____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/


    override fun vote(comment: Comment, vote: Vote) {
        TODO("Not yet implemented")
    }

    override fun save(comment: Comment) {
        TODO("Not yet implemented")
    }

    override fun share(comment: Comment) {
        TODO("Not yet implemented")
    }

    override fun reply(comment: Comment, position: Int) {
        TODO("Not yet implemented")
    }

    override fun viewProfile(comment: Comment) {
        TODO("Not yet implemented")
    }

    override fun report(comment: Comment) {
        TODO("Not yet implemented")
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


//     _____ _                    _____ _ _      _      _      _     _
//    |_   _| |                  / ____| (_)    | |    | |    (_)   | |
//      | | | |_ ___ _ __ ___   | |    | |_  ___| | __ | |     _ ___| |_ ___ _ __   ___ _ __
//      | | | __/ _ \ '_ ` _ \  | |    | | |/ __| |/ / | |    | / __| __/ _ \ '_ \ / _ \ '__|
//     _| |_| ||  __/ | | | | | | |____| | | (__|   <  | |____| \__ \ ||  __/ | | |  __/ |
//    |_____|\__\___|_| |_| |_|  \_____|_|_|\___|_|\_\ |______|_|___/\__\___|_| |_|\___|_|

    override fun itemClicked(item: Item, position: Int) {
        if (item is Post) {
            model.addReadItem(item)
            viewPagerModel.newPage(PostPage(item, position))
        }
    }

//     _           __ _     _____                                             _   _
//    | |         / _| |   |  __ \                                  /\       | | (_)
//    | |     ___| |_| |_  | |  | |_ __ __ ___      _____ _ __     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \  _| __| | |  | | '__/ _` \ \ /\ / / _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___|  __/ | | |_  | |__| | | | (_| |\ V  V /  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//    |______\___|_|  \__| |_____/|_|  \__,_| \_/\_/ \___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/

    @SuppressLint("RtlHardcoded")
    override fun onAddAccountClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSignInFragment())
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    override fun onRemoveAccountClicked(account: SavedAccount) {
        val currentAccount = (requireActivity().application as RedditApplication).currentAccount
        if(account.id == currentAccount?.id){
            saveAccountToPreferences(requireContext(), null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        activityModel.removeAccount(account)
    }

    @SuppressLint("RtlHardcoded")
    override fun onAccountClicked(account: SavedAccount) {
        val currentAccount = (requireActivity().application as RedditApplication).currentAccount
        if(account.id != currentAccount?.id){
            saveAccountToPreferences(requireContext(), account)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        } else {
            model.toggleLeftDrawerExpanding()
        }
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onLogoutClicked() {
        val currentAccount = (requireActivity().application as RedditApplication).currentAccount
        if(currentAccount != null){
            saveAccountToPreferences(requireContext(), null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        } else {
            model.toggleLeftDrawerExpanding()
        }
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onHomeClicked() {
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onMyAccountClicked() {
        if ((activity?.application as RedditApplication).accessToken == null) {
            Snackbar.make(binding.drawerLayout, R.string.please_login, Snackbar.LENGTH_SHORT).show()
        } else {
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(null)))
            binding.drawerLayout.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onInboxClicked() {
        if ((activity?.application as RedditApplication).accessToken == null) {
            Snackbar.make(binding.drawerLayout, R.string.please_login, Snackbar.LENGTH_SHORT).show()
        } else {
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(InboxPage))
            binding.drawerLayout.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onSettingsClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSettingsFragment())
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
        model.fetchFirstPage()
    }

    override fun handleLink(link: String) {
        when(link.getUrlType()){
            UrlType.IMAGE -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.PICTURE)).show(childFragmentManager, null)
            UrlType.GIF -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.GIF)).show(childFragmentManager, null)
            UrlType.GIFV, UrlType.HLS, UrlType.STANDARD_VIDEO -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.VIDEO)).show(childFragmentManager, null)
            UrlType.GFYCAT -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.GFYCAT)).show(childFragmentManager, null)
            UrlType.IMGUR_ALBUM -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.IMGUR_ALBUM)).show(childFragmentManager, null)
//            UrlType.REDDIT_COMMENTS -> viewPagerModel.newPage(ContinueThreadPage(link))
            UrlType.REDDIT_COMMENTS -> TODO("Need to be implemented")
            UrlType.OTHER, UrlType.REDDIT_VIDEO -> activityModel.openChromeTab(link)
        }
    }

    companion object {
        fun newInstance(listing: Listing): ListingFragment {
            return ListingFragment().apply {
                arguments = bundleOf(LISTING_KEY to listing)
            }
        }
    }

}
