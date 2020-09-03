package dev.gtcl.astro.ui.fragments.listing

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.*
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.databinding.*
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.network.Status
import dev.gtcl.astro.ui.ListingScrollListener
import dev.gtcl.astro.ui.LeftDrawerAdapter
import dev.gtcl.astro.ui.ListingAdapter
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.*
import dev.gtcl.astro.ui.fragments.create_post.CreatePostDialogFragment
import dev.gtcl.astro.ui.fragments.manage.ManagePostDialogFragment
import dev.gtcl.astro.ui.fragments.media.MediaDialogFragment
import dev.gtcl.astro.ui.fragments.share.SharePostOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.reply_or_edit.ReplyOrEditDialogFragment
import dev.gtcl.astro.ui.fragments.report.ReportDialogFragment
import dev.gtcl.astro.ui.fragments.share.ShareCommentOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.subscriptions.SubscriptionsDialogFragment
import io.noties.markwon.Markwon


class ListingFragment : Fragment(), PostActions, CommentActions, SubredditActions,
    ItemClickListener, LeftDrawerActions {

    private lateinit var binding: FragmentListingBinding

    private val model: ListingVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(ListingVM::class.java)
    }

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private val markwon: Markwon by lazy {
        createMarkwonInstance(requireContext(), viewPagerModel::linkClicked)
    }

    private lateinit var scrollListener: ListingScrollListener
    private lateinit var listAdapter: ListingAdapter

    override fun onResume() {
        super.onResume()
        viewPagerModel.notifyViewPager()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity().application as AstroApplication)
        val showNsfw = sharedPref.getBoolean(NSFW_KEY, false)
        val blurNsfwThumbnail = sharedPref.getBoolean(NSFW_THUMBNAIL_KEY, false)
        if(showNsfw != model.showNsfw){
            binding.fragmentListingSwipeRefresh.isRefreshing = true
            listAdapter.blurNsfw = blurNsfwThumbnail
            initData()
        } else if(blurNsfwThumbnail != listAdapter.blurNsfw){
            listAdapter.blurNsfw = blurNsfwThumbnail
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListingBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.fragmentListingRightDrawerLayout.lifecycleOwner = viewLifecycleOwner
        binding.model = model

        if (!model.firstPageLoaded) {
            initData()
        }
        initScroller()
        initBottomBar()
        initLeftDrawer()
        initRightDrawer()
        initOtherObservers()

        binding.executePendingBindings()
        return binding.root
    }

    private fun initData(){
        val listing = requireArguments().getParcelable<Listing>(LISTING_KEY)!!
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity().application as AstroApplication)
        val showNsfw = sharedPref.getBoolean(NSFW_KEY, false)
        when(listing){
            is SubredditListing -> model.fetchSubreddit(listing.displayName)
            is SubscriptionListing -> {
                if(listing.subscription.type == SubscriptionType.SUBREDDIT){
                    model.fetchSubreddit(listing.subscription.displayName)
                }
            }
            else -> model.setSubreddit(null)
        }
        model.setListing(listing, showNsfw)
        model.fetchFirstPage()
    }

    private fun initScroller() {
        val listView = binding.fragmentListingList
        val swipeRefresh = binding.fragmentListingSwipeRefresh
        scrollListener = ListingScrollListener(15, listView.layoutManager as GridLayoutManager, model::loadMore)
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val blurNsfw = preferences.getBoolean("blur_nsfw_thumbnail", false)
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        listAdapter = ListingAdapter(
            markwon,
            postActions = this,
            commentActions = this,
            expected = ItemType.Post,
            blurNsfw = blurNsfw,
            itemClickListener = this,
            username = currentAccount?.name){
            listView.apply {
                removeOnScrollListener(scrollListener)
                addOnScrollListener(scrollListener)
                model.retry()
            }
        }
        listView.apply {
            this.adapter = listAdapter
            addOnScrollListener(scrollListener)
        }

        model.items.observe(viewLifecycleOwner, {
            scrollListener.finishedLoading()
            listAdapter.submitList(it)
        })

        model.moreItems.observe(viewLifecycleOwner, {
            if(it != null){
                scrollListener.finishedLoading()
                listAdapter.addItems(it)
                model.moreItemsObserved()
            }
        })

        model.networkState.observe(viewLifecycleOwner, {
            listAdapter.networkState = it
            if (it == NetworkState.LOADED || it.status == Status.FAILED) {
                swipeRefresh.isRefreshing = false
            }
        })

        model.lastItemReached.observe(viewLifecycleOwner, {
            if (it == true) {
                listView.removeOnScrollListener(scrollListener)
            }
        })

        swipeRefresh.setOnRefreshListener {
            if(model.networkState.value == NetworkState.LOADING){
                swipeRefresh.isRefreshing = false
                return@setOnRefreshListener
            }

            listView.apply {
                removeOnScrollListener(scrollListener)
                addOnScrollListener(scrollListener)
            }
            initData()
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun initLeftDrawer() {
        val leftDrawerAdapter = LeftDrawerAdapter(requireContext(), this, LeftDrawerHeader.HOME)
        val leftDrawerLayout = binding.fragmentListingLeftDrawerLayout
        leftDrawerLayout.layoutLeftDrawerList.adapter = leftDrawerAdapter
        leftDrawerLayout.account = (requireActivity().application as AstroApplication).currentAccount

        activityModel.allUsers.observe(viewLifecycleOwner, {
            leftDrawerAdapter.submitUsers(it)
        })

        leftDrawerLayout.layoutLeftDrawerBanner.setOnClickListener {
            leftDrawerAdapter.toggleExpanded()
            rotateView(leftDrawerLayout.layoutLeftDrawerExpandedIndicator, leftDrawerAdapter.isExpanded)
        }

        binding.fragmentListingTopAppBarLayout.layoutTopAppBarListingToolbar.setNavigationOnClickListener {
            binding.fragmentListingDrawer.openDrawer(Gravity.LEFT)
        }

    }

    @SuppressLint("RtlHardcoded")
    private fun initRightDrawer() {
        val topAppBar = binding.fragmentListingTopAppBarLayout
        val drawer = binding.fragmentListingDrawer
        val rightDrawerLayout = binding.fragmentListingRightDrawerLayout
        topAppBar.layoutTopAppBarListingSideBarButton.setOnClickListener {
            drawer.openDrawer(Gravity.RIGHT)
        }

        rightDrawerLayout.lifecycleOwner = this
        model.subreddit.observe(viewLifecycleOwner, { sub ->
            if (sub != null) {
                rightDrawerLayout.layoutRightDrawerSubscribeToggle.iconSubscribeBackground.setOnClickListener {
                    checkedIfLoggedInBeforeExecuting(requireContext()) {
                        subscribe(sub, !(sub.userSubscribed ?: false))
                        rightDrawerLayout.invalidateAll()
                    }
                }
                drawer.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    Gravity.RIGHT
                )
                if(sub.banner == null){
                    topAppBar.layoutTopAppBarListingCollapsingToolbar.contentScrim = null
                    val typedValue = TypedValue()
                    val theme = requireContext().theme
                    theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
                    topAppBar.layoutTopAppBarListingToolbar.setBackgroundColor(typedValue.data)
                }
                markwon.setMarkdown(rightDrawerLayout.layoutRightDrawerPublicDescription, sub.publicDescription + "\n\n" + sub.description)
            } else {
                rightDrawerLayout.layoutRightDrawerSubscribeToggle.iconSubscribeBackground.isClickable = false
                drawer.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    Gravity.RIGHT
                )
                topAppBar.layoutTopAppBarListingCollapsingToolbar.contentScrim = null
                val typedValue = TypedValue()
                val theme = requireContext().theme
                theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
                topAppBar.layoutTopAppBarListingToolbar.setBackgroundColor(typedValue.data)
            }
        })
    }

    private fun initBottomBar() {
        val bottomBarLayout = binding.fragmentListingBottomAppBarLayout
        bottomBarLayout.layoutListingBottomBarSortButton.setOnClickListener {anchor ->
            val currentSort = model.postSort.value!!
            val currentTime = model.time.value
            val onSortSelected: (PostSort) -> Unit = { postSortSelected ->
                if(model.listing is SearchListing || postSortSelected == PostSort.TOP || postSortSelected == PostSort.CONTROVERSIAL){
                    val time = if(currentSort == postSortSelected) currentTime else null
                    showTimePopup(anchor, time){ timeSortSelected ->
                        model.setSort(postSortSelected, timeSortSelected)
                        model.fetchFirstPage()
                    }
                } else {
                    model.setSort(postSortSelected)
                    model.fetchFirstPage()
                }
            }
            if(model.listing is SearchListing){
                showSearchSortPopup(anchor, currentSort, onSortSelected)
            } else {
                showPostSortPopup(anchor, currentSort, onSortSelected)
            }
        }

        bottomBarLayout.layoutListingBottomBarSubredditButton.setOnClickListener {
            SubscriptionsDialogFragment().show(childFragmentManager, null)
        }

        bottomBarLayout.layoutListingBottomBarRefreshButton.setOnClickListener {
            initData()
        }

        bottomBarLayout.layoutListingBottomBarMoreOptionsButton.setOnClickListener {
            showMoreOptionsPopup(it)
        }
    }

    @SuppressLint("WrongConstant")
    private fun initOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, {
            if (it != null) {
                binding.fragmentListingDrawer.apply {
                    closeDrawer(Gravity.START)
                    closeDrawer(Gravity.END)
                }
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        })

        childFragmentManager.setFragmentResultListener(LISTING_KEY, viewLifecycleOwner, { _, bundle ->
            val listing = bundle.get(LISTING_KEY) as Listing
            if(listing is SubscriptionListing && listing.subscription.type == SubscriptionType.USER){
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(listing.subscription.displayName)))
            } else {
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(ListingPage(listing)))
            }
        })

        childFragmentManager.setFragmentResultListener(REPORT_KEY, viewLifecycleOwner, { _, bundle ->
            val  position = bundle.getInt(POSITION_KEY, -1)
            if(position != -1){
                model.removeItemAt(position)
                listAdapter.removeAt(position)
            }
        })

        childFragmentManager.setFragmentResultListener(MANAGE_POST_KEY, viewLifecycleOwner,
            { _, bundle ->
                val position = bundle.getInt(POST_KEY)
                val nsfw = bundle.getBoolean(NSFW_KEY)
                val spoiler = bundle.getBoolean(SPOILER_KEY)
                val getNotification = bundle.getBoolean(GET_NOTIFICATIONS_KEY)
                val flair = bundle.get(FLAIRS_KEY) as Flair?
                val post = model.items.value!![position] as Post
                activityModel.updatePost(post, nsfw, spoiler, getNotification, flair)
                listAdapter.notifyItemChanged(position)
            })

        childFragmentManager.setFragmentResultListener(NEW_REPLY_KEY, viewLifecycleOwner,
            { _, bundle ->
                val item = bundle.get(ITEM_KEY) as Item
                val position = bundle.getInt(POSITION_KEY)
                val reply = bundle.getBoolean(NEW_REPLY_KEY)
                if(!reply) {
                    model.updateItemAt(position, item)
                    listAdapter.updateAt(position, item)
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
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            post.updateScore(vote)
            activityModel.vote(post.name, vote)
        }
    }

    override fun share(post: Post) {
        SharePostOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
    }

    override fun viewProfile(post: Post) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(post.author)
            )
        )
    }

    override fun save(post: Post) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            post.saved = post.saved != true
            activityModel.save(post.name, post.saved)
        }
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
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            post.hidden = !post.hidden
            activityModel.hide(post.name, post.hidden)
            model.removeItemAt(position)
            listAdapter.removeAt(position)
        }
    }

    override fun report(post: Post, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            ReportDialogFragment.newInstance(post, position).show(childFragmentManager, null)
        }
    }

    override fun thumbnailClicked(post: Post, position: Int) {
        model.addReadItem(post)
        when (val urlType: UrlType? = post.url?.getUrlType()) {
            UrlType.OTHER -> activityModel.openChromeTab(post.url)
            UrlType.REDDIT_GALLERY -> {
                val dialog = MediaDialogFragment.newInstance(post.url, post.galleryAsMediaItems!!)
                dialog.show(parentFragmentManager, null)
            }
            null -> throw IllegalArgumentException("Post does not have URL")
            else -> {
                val mediaType = when (urlType) {
                    UrlType.IMGUR_ALBUM -> MediaType.IMGUR_ALBUM
                    UrlType.GIF -> MediaType.GIF
                    UrlType.GFYCAT -> MediaType.GFYCAT
                    UrlType.REDGIFS -> MediaType.REDGIFS
                    UrlType.IMAGE -> MediaType.PICTURE
                    UrlType.HLS, UrlType.GIFV, UrlType.STANDARD_VIDEO, UrlType.REDDIT_VIDEO -> MediaType.VIDEO
                    else -> throw IllegalArgumentException("Invalid media type: $urlType")
                }
                val url = when (mediaType) {
                    MediaType.VIDEO -> post.previewVideoUrl!!
                    else -> post.url
                }
                val backupUrl = when (mediaType) {
                    MediaType.GFYCAT, MediaType.REDGIFS -> post.previewVideoUrl
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

    override fun edit(post: Post, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            ReplyOrEditDialogFragment.newInstance(post, position, false).show(childFragmentManager, null)
        }
    }

    override fun manage(post: Post, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            ManagePostDialogFragment.newInstance(post, position).show(childFragmentManager, null)
        }
    }

    override fun delete(post: Post, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.delete(post.name)
            model.removeItemAt(position)
            listAdapter.removeAt(position)
        }
    }

//      _____                                     _                  _   _
//     / ____|                                   | |       /\       | | (_)
//    | |     ___  _ __ ___  _ __ ___   ___ _ __ | |_     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___| (_) | | | | | | | | | | |  __/ | | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//     \_____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/


    override fun vote(comment: Comment, vote: Vote) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            comment.updateScore(vote)
            activityModel.vote(comment.name, vote)
        }
    }

    override fun save(comment: Comment) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            comment.saved = comment.saved != true
            activityModel.save(comment.name, comment.saved == true)
        }
    }

    override fun share(comment: Comment) {
        ShareCommentOptionsDialogFragment.newInstance(comment).show(parentFragmentManager, null)
    }

    override fun reply(comment: Comment, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            if(comment.locked == true || comment.deleted){
                Toast.makeText(requireContext(), R.string.cannot_reply_to_comment, Toast.LENGTH_LONG).show()
            } else {
                ReplyOrEditDialogFragment.newInstance(comment, position, true).show(childFragmentManager, null)
            }
        }
    }

    override fun viewProfile(comment: Comment) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(comment.author)
            )
        )
    }

    override fun report(comment: Comment, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            ReportDialogFragment.newInstance(comment, position).show(childFragmentManager, null)
        }
    }

    override fun edit(comment: Comment, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            ReplyOrEditDialogFragment.newInstance(comment, position, false).show(childFragmentManager, null)
        }
    }

    override fun delete(comment: Comment, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.delete(comment.name)
            model.removeItemAt(position)
            listAdapter.removeAt(position)
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
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            subreddit.userSubscribed = subscribe
            activityModel.subscribe(subreddit, subscribe)
        }
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
            activityModel.newPage(PostPage(item, position))
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
        binding.fragmentListingDrawer.closeDrawer(Gravity.LEFT)
    }

    override fun onRemoveAccountClicked(account: SavedAccount) {
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        if(account.id == currentAccount?.id){
            saveAccountToPreferences(requireContext(), null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        activityModel.removeAccount(account)
    }

    @SuppressLint("RtlHardcoded")
    override fun onAccountClicked(account: SavedAccount) {
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        if(account.id != currentAccount?.id){
            saveAccountToPreferences(requireContext(), account)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding.fragmentListingDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onLogoutClicked() {
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        if(currentAccount != null){
            saveAccountToPreferences(requireContext(), null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding.fragmentListingDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onHomeClicked() {
        binding.fragmentListingDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onMyAccountClicked() {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(null)))
            binding.fragmentListingDrawer.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onInboxClicked() {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(InboxPage))
            binding.fragmentListingDrawer.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onSettingsClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSettingsFragment())
        binding.fragmentListingDrawer.closeDrawer(Gravity.LEFT)
    }


//     _____                         __          ___           _
//    |  __ \                        \ \        / (_)         | |
//    | |__) |__  _ __  _   _ _ __    \ \  /\  / / _ _ __   __| | _____      _____
//    |  ___/ _ \| '_ \| | | | '_ \    \ \/  \/ / | | '_ \ / _` |/ _ \ \ /\ / / __|
//    | |  | (_) | |_) | |_| | |_) |    \  /\  /  | | | | | (_| | (_) \ V  V /\__ \
//    |_|   \___/| .__/ \__,_| .__/      \/  \/   |_|_| |_|\__,_|\___/ \_/\_/ |___/
//               | |         | |
//               |_|         |_|

    private fun showPostSortPopup(anchor: View, currentSort: PostSort, onSortSelected: (PostSort) -> Unit){
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupPostSortBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            sort = currentSort
            popupPostSortBest.root.setOnClickListener {
                onSortSelected(PostSort.BEST)
                popupWindow.dismiss()
            }
            popupPostSortHot.root.setOnClickListener {
                onSortSelected(PostSort.HOT)
                popupWindow.dismiss()
            }
            popupPostSortNew.root.setOnClickListener {
                onSortSelected(PostSort.NEW)
                popupWindow.dismiss()
            }
            popupPostSortTop.root.setOnClickListener {
                onSortSelected(PostSort.TOP)
                popupWindow.dismiss()
            }
            popupPostSortControversial.root.setOnClickListener {
                onSortSelected(PostSort.CONTROVERSIAL)
                popupWindow.dismiss()
            }
            popupPostSortRising.root.setOnClickListener {
                onSortSelected(PostSort.RISING)
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        popupWindow.showAsDropdown(anchor, popupBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, popupBinding.root.measuredHeight)
    }

    private fun showSearchSortPopup(anchor: View, currentSort: PostSort, onSortSelected: (PostSort) -> Unit){
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupSearchSortBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            sort = currentSort
            popupSearchSortMostRelevant.root.setOnClickListener {
                onSortSelected(PostSort.RELEVANCE)
                popupWindow.dismiss()
            }
            popupSearchSortHot.root.setOnClickListener {
                onSortSelected(PostSort.HOT)
                popupWindow.dismiss()
            }
            popupSearchSortNew.root.setOnClickListener {
                onSortSelected(PostSort.NEW)
                popupWindow.dismiss()
            }
            popupSearchSortTop.root.setOnClickListener {
                onSortSelected(PostSort.TOP)
                popupWindow.dismiss()
            }
            popupSearchSortCommentCount.root.setOnClickListener {
                onSortSelected(PostSort.COMMENTS)
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        popupWindow.showAsDropdown(anchor, popupBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, popupBinding.root.measuredHeight)
    }

    private fun showTimePopup(anchor: View, currentTimeSort: Time?, onTimeSelected: (Time) -> Unit){
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupTimeSortBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            time = currentTimeSort
            popupTimeSortHour.root.setOnClickListener {
                onTimeSelected(Time.HOUR)
                popupWindow.dismiss()
            }
            popupTimeSortDay.root.setOnClickListener {
                onTimeSelected(Time.DAY)
                popupWindow.dismiss()
            }
            popupTimeSortWeek.root.setOnClickListener {
                onTimeSelected(Time.WEEK)
                popupWindow.dismiss()
            }
            popupTimeSortMonth.root.setOnClickListener {
                onTimeSelected(Time.MONTH)
                popupWindow.dismiss()
            }
            popupTimeSortYear.root.setOnClickListener {
                onTimeSelected(Time.YEAR)
                popupWindow.dismiss()
            }
            popupTimeSortAll.root.setOnClickListener {
                onTimeSelected(Time.ALL)
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        popupWindow.showAsDropdown(anchor, popupBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, popupBinding.root.measuredHeight)
    }

    private fun showMoreOptionsPopup(anchor: View){
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupListingActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            popupListingActionsCreatePost.root.setOnClickListener {
                if ((activity?.application as AstroApplication).accessToken == null) {
                    Snackbar.make(binding.fragmentListingDrawer, R.string.must_be_logged_in, Snackbar.LENGTH_SHORT).show()
                } else {
                    val subredditName = model.subreddit.value?.displayName
                    CreatePostDialogFragment.newInstance(subredditName).show(parentFragmentManager, null)
                }
                popupWindow.dismiss()
            }
            popupListingActionsSearch.root.setOnClickListener {
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSearchFragment(false))
                popupWindow.dismiss()
            }
            popupListingActionsMyAccount.root.setOnClickListener {
                onMyAccountClicked()
                popupWindow.dismiss()
            }
            popupListingActionsInbox.root.setOnClickListener {
                onInboxClicked()
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        popupWindow.showAsDropdown(anchor, popupBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, popupBinding.root.measuredHeight)
    }

    companion object {
        fun newInstance(listing: Listing): ListingFragment {
            return ListingFragment().apply {
                arguments = bundleOf(LISTING_KEY to listing)
            }
        }
    }

}
