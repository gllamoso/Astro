package dev.gtcl.astro.ui.fragments.post_listing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.*
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.databinding.*
import dev.gtcl.astro.html.createHtmlViews
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.network.Status
import dev.gtcl.astro.ui.LeftDrawerAdapter
import dev.gtcl.astro.ui.LeftDrawerHeader
import dev.gtcl.astro.ui.ListingAdapter
import dev.gtcl.astro.ui.ListingScrollListener
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.create_post.CreatePostDialogFragment
import dev.gtcl.astro.ui.fragments.manage.ManagePostDialogFragment
import dev.gtcl.astro.ui.fragments.reply_or_edit.ReplyOrEditDialogFragment
import dev.gtcl.astro.ui.fragments.report.ReportDialogFragment
import dev.gtcl.astro.ui.fragments.search.SimpleItemAdapter
import dev.gtcl.astro.ui.fragments.share.ShareCommentOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.share.SharePostOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.subreddits.SubredditInfoDialogFragment
import dev.gtcl.astro.ui.fragments.subscriptions.SubscriptionsDialogFragment
import dev.gtcl.astro.ui.fragments.view_pager.*


class PostListingFragment : Fragment(), PostActions, CommentActions, SubredditActions,
    ItemClickListener, LeftDrawerActions {

    private var binding: FragmentListingBinding? = null

    private val model: PostListingVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(PostListingVM::class.java)
    }

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private val sharedPref by lazy {
        (requireActivity().application as AstroApplication).sharedPref
    }

    private lateinit var scrollListener: ListingScrollListener
    private lateinit var listAdapter: ListingAdapter

    private val movementMethod by lazy {
        createBetterLinkMovementInstance(requireContext(), findNavController(), parentFragmentManager, activityModel)
    }

    override fun onResume() {
        super.onResume()
        viewPagerModel.syncViewPager()
        val showNsfw = sharedPref.getBoolean(NSFW_KEY, false)
        val blurNsfwThumbnail = sharedPref.getBoolean(NSFW_THUMBNAIL_KEY, false)
        if (showNsfw != model.showNsfw) {
            binding?.fragmentListingSwipeRefresh?.isRefreshing = true
            listAdapter.blurNsfw = blurNsfwThumbnail
            initData(false)
        } else if (blurNsfwThumbnail != listAdapter.blurNsfw) {
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
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.fragmentListingTopAppBarLayout?.lifecycleOwner = viewLifecycleOwner
        binding?.fragmentListingRightDrawerLayout?.lifecycleOwner = viewLifecycleOwner
        binding?.model = model

        if (!model.initialPageLoaded) {
            initData(true)
        }
        initScroller()
        initBottomBar()
        initLeftDrawer()
        initRightDrawerAndTopBar()
        initOtherObservers()

        binding?.executePendingBindings()
        return binding?.root
    }

    private fun initData(loadDefaults: Boolean) {
        val listing = requireArguments().getParcelable<PostListing>(LISTING_KEY) ?: return
        val showNsfw = sharedPref.getBoolean(NSFW_KEY, false)
        when (listing) {
            FrontPage, All, Popular, is SearchListing, is ProfileListing -> {
                model.fetchTrendingSubreddits()
            }
            is SubredditListing -> model.fetchSubreddit(listing.displayName)
            is MultiRedditListing -> model.fetchMultiReddit(listing.path)
        }
        model.setListingInfo(listing, loadDefaults)
        model.setNsfw(showNsfw)
        model.fetchFirstPage()
    }

    private fun resetOnScrollListener() {
        binding?.fragmentListingList?.apply {
            clearOnScrollListeners()
            addOnScrollListener(scrollListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun initScroller() {
        val listView = binding?.fragmentListingList
        val swipeRefresh = binding?.fragmentListingSwipeRefresh
        scrollListener =
            ListingScrollListener(15, listView?.layoutManager as GridLayoutManager, model::loadMore)
        val blurNsfw = sharedPref.getBoolean("blur_nsfw_thumbnail", false)
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        listAdapter = ListingAdapter(
            movementMethod = movementMethod,
            postActions = this,
            commentActions = this,
            expected = ItemType.Post,
            blurNsfw = blurNsfw,
            itemClickListener = this,
            username = currentAccount?.name
        ) {
            resetOnScrollListener()
            model.retry()
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
            if (it != null) {
                scrollListener.finishedLoading()
                listAdapter.addItems(it)
                model.moreItemsObserved()
            }
        })

        model.networkState.observe(viewLifecycleOwner, {
            listAdapter.networkState = it
            if (it == NetworkState.LOADED || it.status == Status.FAILED) {
                swipeRefresh?.isRefreshing = false
            }
        })

        model.lastItemReached.observe(viewLifecycleOwner, {
            if (it == true) {
                listView.clearOnScrollListeners()
            }
        })

        swipeRefresh?.setOnRefreshListener {
            if (model.networkState.value == NetworkState.LOADING) {
                swipeRefresh.isRefreshing = false
                return@setOnRefreshListener
            }

            resetOnScrollListener()
            initData(false)
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun initLeftDrawer() {
        val leftDrawerAdapter = LeftDrawerAdapter(requireContext(), this, LeftDrawerHeader.HOME)
        val leftDrawerLayout = binding?.fragmentListingLeftDrawerLayout
        leftDrawerLayout?.layoutLeftDrawerList?.adapter = leftDrawerAdapter
        leftDrawerLayout?.account =
            (requireActivity().application as AstroApplication).currentAccount

        activityModel.allUsers.observe(viewLifecycleOwner, {
            leftDrawerAdapter.submitUsers(it)
        })

        leftDrawerLayout?.layoutLeftDrawerBanner?.setOnClickListener {
            leftDrawerAdapter.toggleExpanded()
            rotateView(
                leftDrawerLayout.layoutLeftDrawerExpandedIndicator,
                leftDrawerAdapter.isExpanded
            )
        }

        binding?.fragmentListingTopAppBarLayout?.layoutTopAppBarListingToolbar?.setNavigationOnClickListener {
            binding?.fragmentListingDrawer?.openDrawer(Gravity.LEFT)
        }

    }

    @SuppressLint("RtlHardcoded")
    private fun initRightDrawerAndTopBar() {
        val listing = requireArguments().getParcelable<PostListing>(LISTING_KEY) ?: return
        val topAppBar = binding?.fragmentListingTopAppBarLayout
        val drawer = binding?.fragmentListingDrawer
        val rightDrawerLayout = binding?.fragmentListingRightDrawerLayout
        topAppBar?.layoutTopAppBarListingSideBarButton?.setOnClickListener {
            drawer?.openDrawer(Gravity.RIGHT)
        }

        val topRightIcon = when (listing) {
            FrontPage, All, Popular, is SearchListing, is ProfileListing -> R.drawable.ic_trending_up_colored_24
            is MultiRedditListing -> R.drawable.ic_collection_colored_24
            else -> R.drawable.ic_saturn_colored_24
        }

        // Set tinted sidebar button
        val drawable = AppCompatResources.getDrawable(requireContext(), topRightIcon) ?: return
        topAppBar?.layoutTopAppBarListingSideBarButton?.setImageDrawable(drawable)

        model.subreddit.observe(viewLifecycleOwner, { sub ->
            if (sub != null) {
                rightDrawerLayout?.apply {
                    iconImg = sub.icon ?: ""
                    name = sub.displayName.replaceFirst("u_", "u/")
                    title = sub.titleFormatted
                    subscribers = if (sub.subscribers ?: 0 > 0) sub.subscribers else null
                    subscribed = sub.userSubscribed
                    layoutRightDrawerSubscribeToggle.iconSubscribeBackground.setOnClickListener {
                        checkIfLoggedInBeforeExecuting(requireContext()) {
                            val subscribe = !(sub.userSubscribed ?: false)
                            sub.userSubscribed = subscribe
                            activityModel.subscribe(sub, subscribe)
                            subscribed = sub.userSubscribed
                            rightDrawerLayout.invalidateAll()
                        }
                    }
                }
                if (sub.banner == null) {
                    topAppBar?.layoutTopAppBarListingCollapsingToolbar?.contentScrim = null
                }
                sub.icon?.let {
                    loadTopBarIcon(it, drawable)
                }
                rightDrawerLayout?.layoutRightDrawerPublicDescriptionLayout?.createHtmlViews(
                    sub.parseDescription(),
                        binding?.fragmentListingDrawer, movementMethod
                )

                if (sub.displayName.startsWith("u_")) {
                    rightDrawerLayout?.layoutRightDrawerSubIcon?.setOnClickListener {
                        binding?.fragmentListingDrawer?.closeDrawer(Gravity.RIGHT)
                        findNavController().navigate(
                            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                                AccountPage(sub.displayName.removePrefix("u_"))
                            )
                        )
                    }
                }
            }
        })

        model.trendingSubreddits.observe(viewLifecycleOwner, {
            if (it != null) {
                val adapter = SimpleItemAdapter(this, this)
                binding?.fragmentListingRightDrawerLayout?.layoutRightDrawerSubreddits?.adapter =
                    adapter
                adapter.submitList(it)
                binding?.fragmentListingRightDrawerLayout?.layoutRightDrawerPublicDescriptionLayout?.visibility =
                    View.GONE
                binding?.fragmentListingRightDrawerLayout?.name =
                    getString(R.string.trending_subreddits)
            }
        })

        model.multiReddit.observe(viewLifecycleOwner, { multi ->
            if (multi != null) {
                binding?.fragmentListingRightDrawerLayout?.apply {
                    iconImg = multi.iconUrl
                    name = multi.displayName
                    title = when (multi.visibility) {
                        Visibility.PUBLIC -> getString(R.string.public_label)
                        Visibility.HIDDEN -> getString(R.string.hidden)
                        Visibility.PRIVATE -> getString(R.string.private_label)
                    }
                    editable = multi.canEdit
                    layoutRightDrawerPublicDescriptionLayout.createHtmlViews(
                        multi.parseDescription(),
                        binding?.fragmentListingDrawer,
                        movementMethod
                    )
                }

                val adapter = SimpleItemAdapter(this, this)
                binding?.fragmentListingRightDrawerLayout?.layoutRightDrawerSubreddits?.adapter =
                    adapter
                adapter.submitList(multi.subreddits.mapNotNull { it.data })

                loadTopBarIcon(multi.iconUrl, drawable)

                binding?.fragmentListingRightDrawerLayout?.layoutRightDrawerEditButton?.setOnClickListener {
                    findNavController().navigate(
                        ViewPagerFragmentDirections.actionViewPagerFragmentToMultiRedditFragment(
                            multi.pathFormatted
                        )
                    )
                }
            }
        })

        model.sidebarError.observe(viewLifecycleOwner, {
            if(it != null){
                binding?.fragmentListingRightDrawerLayout?.name = it
                model.sidebarErrorObserved()
            }
        })

    }

    private fun loadTopBarIcon(imgUrl: String, placeholder: Drawable) {
        val imageView =
            (binding?.fragmentListingTopAppBarLayout?.layoutTopAppBarListingSideBarButton ?: return)

        GlideApp.with(requireContext())
            .load(imgUrl)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply(
                RequestOptions()
                    .placeholder(placeholder)
                    .error(placeholder)
                    .circleCrop()
            )
            .into(imageView)
    }

    private fun initBottomBar() {
        val bottomBarLayout = binding?.fragmentListingBottomAppBarLayout
        bottomBarLayout?.layoutListingBottomBarSortButton?.setOnClickListener { anchor ->
            val currentSort = model.postSort.value ?: return@setOnClickListener
            val currentTime = model.time.value
            val onSortSelected: (PostSort) -> Unit = { postSortSelected ->
                if (model.postListing is SearchListing || postSortSelected == PostSort.TOP || postSortSelected == PostSort.CONTROVERSIAL) {
                    val time = if (currentSort == postSortSelected) currentTime else null
                    showTimePopup(anchor, time) { timeSortSelected ->
                        resetOnScrollListener()
                        model.setListingSort(postSortSelected, timeSortSelected)
                        model.fetchFirstPage()
                    }
                } else {
                    resetOnScrollListener()
                    model.setListingSort(postSortSelected)
                    model.fetchFirstPage()
                }
            }
            if (model.postListing is SearchListing) {
                showSearchSortPopup(anchor, currentSort, onSortSelected)
            } else {
                showPostSortPopup(anchor, currentSort, onSortSelected)
            }
        }

        bottomBarLayout?.layoutListingBottomBarSubredditButton?.setOnClickListener {
            SubscriptionsDialogFragment().show(childFragmentManager, null)
        }

        bottomBarLayout?.layoutListingBottomBarRefreshButton?.setOnClickListener {
            if (model.networkState.value == NetworkState.LOADING) {
                return@setOnClickListener
            }
            resetOnScrollListener()
            initData(false)
        }

        bottomBarLayout?.layoutListingBottomBarMoreOptionsButton?.setOnClickListener {
            showMoreOptionsPopup(it)
        }
    }

    @SuppressLint("WrongConstant", "RtlHardcoded")
    private fun initOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                binding?.fragmentListingDrawer?.apply {
                    closeDrawer(Gravity.START)
                    closeDrawer(Gravity.END)
                }
                model.errorMessageObserved()
            }
        })

        childFragmentManager.setFragmentResultListener(
            LISTING_KEY,
            viewLifecycleOwner,
            { _, bundle ->
                val listing = bundle.get(LISTING_KEY) as PostListing
                if (listing is ProfileListing && listing.user != null) {
                    findNavController().navigate(
                        ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                            AccountPage(listing.user)
                        )
                    )
                } else {
                    findNavController().navigate(
                        ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                            ListingPage(listing)
                        )
                    )
                }
            })

        childFragmentManager.setFragmentResultListener(
            REPORT_KEY,
            viewLifecycleOwner,
            { _, bundle ->
                val position = bundle.getInt(POSITION_KEY, -1)
                if (position != -1) {
                    model.removeItemAt(position)
                    listAdapter.removeAt(position)
                }
            })

        childFragmentManager.setFragmentResultListener(MANAGE_POST_KEY, viewLifecycleOwner,
            { _, bundle ->
                val position = bundle.getInt(POSITION_KEY)
                val nsfw = bundle.getBoolean(NSFW_KEY)
                val spoiler = bundle.getBoolean(SPOILER_KEY)
                val getNotification = bundle.getBoolean(GET_NOTIFICATIONS_KEY)
                val flair = bundle.get(FLAIRS_KEY) as Flair?
                val post = (model.items.value ?: return@setFragmentResultListener)[position] as Post
                activityModel.updatePost(post, nsfw, spoiler, getNotification, flair)
                listAdapter.notifyItemChanged(position)
            })

        childFragmentManager.setFragmentResultListener(NEW_REPLY_KEY, viewLifecycleOwner,
            { _, bundle ->
                val item = bundle.get(ITEM_KEY) as Item
                val position = bundle.getInt(POSITION_KEY)
                val reply = bundle.getBoolean(NEW_REPLY_KEY)
                if (!reply) {
                    model.updateItemAt(position, item)
                    listAdapter.updateAt(position, item)
                }
            })

        activityModel.subredditSelected.observe(viewLifecycleOwner, {
            if (it != null) {
                binding?.fragmentListingDrawer?.closeDrawer(Gravity.RIGHT)
                findNavController().navigate(
                    ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                        ListingPage(SubredditListing(it.displayName))
                    )
                )
                activityModel.subredditObserved()
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
        checkIfLoggedInBeforeExecuting(requireContext()) {
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
        checkIfLoggedInBeforeExecuting(requireContext()) {
            post.saved = post.saved != true
            activityModel.save(post.name, post.saved)
        }
    }

    override fun subredditSelected(sub: String) {
        model.postListing.let {
            if (it is SubredditListing && it.displayName == sub) {
                return
            }
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                    ListingPage(SubredditListing(sub))
                )
            )
        }
    }

    override fun hide(post: Post, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            post.hidden = !post.hidden
            activityModel.hide(post.name, post.hidden)
            model.removeItemAt(position)
            listAdapter.removeAt(position)
        }
    }

    override fun report(post: Post, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ReportDialogFragment.newInstance(post, position).show(childFragmentManager, null)
        }
    }

    override fun thumbnailClicked(post: Post, position: Int) {
        post.isRead = true
        model.addReadItem(post)
        if(post.isSelf){
            activityModel.newViewPagerPage(PostPage(post, position))
        } else {
            post.urlFormatted?.handleUrl(context, PostPage(post, position), post.previewVideoUrl, parentFragmentManager, findNavController(), activityModel)
        }
    }

    override fun edit(post: Post, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ReplyOrEditDialogFragment.newInstance(post, position, false)
                .show(childFragmentManager, null)
        }
    }

    override fun manage(post: Post, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ManagePostDialogFragment.newInstance(post, position).show(childFragmentManager, null)
        }
    }

    override fun delete(post: Post, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
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
        checkIfLoggedInBeforeExecuting(requireContext()) {
            comment.updateScore(vote)
            activityModel.vote(comment.name, vote)
        }
    }

    override fun save(comment: Comment) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            comment.saved = comment.saved != true
            activityModel.save(comment.name, comment.saved == true)
        }
    }

    override fun share(comment: Comment) {
        ShareCommentOptionsDialogFragment.newInstance(comment).show(parentFragmentManager, null)
    }

    override fun reply(comment: Comment, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            if (comment.locked == true || comment.deleted) {
                Toast.makeText(
                    requireContext(),
                    R.string.cannot_reply_to_comment,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                ReplyOrEditDialogFragment.newInstance(comment, -1, true)
                    .show(childFragmentManager, null)
            }
        }
    }

    override fun mark(comment: Comment) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            comment.new = !(comment.new ?: false)
            activityModel.markMessage(comment, !(comment.new ?: false))
        }
    }

    override fun block(comment: Comment, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.block(comment)
            listAdapter.removeAt(position)
            model.removeItemAt(position)
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
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ReportDialogFragment.newInstance(comment, position).show(childFragmentManager, null)
        }
    }

    override fun edit(comment: Comment, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ReplyOrEditDialogFragment.newInstance(comment, position, false)
                .show(childFragmentManager, null)
        }
    }

    override fun delete(comment: Comment, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
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

    override fun viewMoreInfo(displayName: String) {
        if (displayName.startsWith("u_")) {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                    AccountPage(displayName.removePrefix("u_"))
                )
            )
        } else {
            SubredditInfoDialogFragment.newInstance(displayName).show(childFragmentManager, null)
        }
    }


//     _____ _                    _____ _ _      _      _      _     _
//    |_   _| |                  / ____| (_)    | |    | |    (_)   | |
//      | | | |_ ___ _ __ ___   | |    | |_  ___| | __ | |     _ ___| |_ ___ _ __   ___ _ __
//      | | | __/ _ \ '_ ` _ \  | |    | | |/ __| |/ / | |    | / __| __/ _ \ '_ \ / _ \ '__|
//     _| |_| ||  __/ | | | | | | |____| | | (__|   <  | |____| \__ \ ||  __/ | | |  __/ |
//    |_____|\__\___|_| |_| |_|  \_____|_|_|\___|_|\_\ |______|_|___/\__\___|_| |_|\___|_|

    @SuppressLint("RtlHardcoded")
    override fun itemClicked(item: Item, position: Int) {
        when (item) {
            is Post -> {
                item.isRead = true
                model.addReadItem(item)
                activityModel.newViewPagerPage(PostPage(item, position))
            }
            is Message -> { // Should never happen
                ReplyOrEditDialogFragment.newInstance(item, -1, true)
                    .show(childFragmentManager, null)
            }
            is Comment -> {
                if (item.permalinkWithRedditDomain != null) {
                    activityModel.newViewPagerPage(
                        CommentsPage(
                            item.permalinkWithRedditDomain,
                            true
                        )
                    )
                } else {
                    val context = item.contextFormatted
                    if (context.isNullOrBlank()) {
                        throw Exception("Comment has no permalink or context: $item")
                    }
                    val regex = "/[a-z0-9]+/\\?context=[0-9]+".toRegex()
                    val link = if (item.parentId?.startsWith("t1_") == true) {
                        context.replace(regex, item.parentId.replace("t1_", "/"))
                    } else {
                        context
                    }
                    activityModel.newViewPagerPage(
                        CommentsPage(
                            "https://www.reddit.com$link",
                            true
                        )
                    )
                }
            }
            is Subreddit -> {
                binding?.fragmentListingDrawer?.closeDrawer(Gravity.RIGHT)
                findNavController().navigate(
                    ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                        ListingPage(SubredditListing(item.displayName))
                    )
                )
            }
        }
    }

    override fun itemLongClicked(item: Item, position: Int) {
        item.isExpanded = !item.isExpanded
        listAdapter.notifyItemChanged(position)
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
        binding?.fragmentListingDrawer?.closeDrawer(Gravity.LEFT)
    }

    override fun onRemoveAccountClicked(account: SavedAccount) {
        val application = (requireActivity().application as AstroApplication)
        val currentAccount = application.currentAccount
        if (account.id == currentAccount?.id) {
            application.saveAccount(null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        activityModel.removeAccount(account)
    }

    @SuppressLint("RtlHardcoded")
    override fun onAccountClicked(account: SavedAccount) {
        val application = requireActivity().application as AstroApplication
        val currentAccount = application.currentAccount
        if (account.id != currentAccount?.id) {
            application.saveAccount(account)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding?.fragmentListingDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onLogoutClicked() {
        val application = (requireActivity().application as AstroApplication)
        val currentAccount = application.currentAccount
        if (currentAccount != null) {
            application.saveAccount(null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding?.fragmentListingDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onHomeClicked() {
        binding?.fragmentListingDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onMyAccountClicked() {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                    AccountPage(null)
                )
            )
            binding?.fragmentListingDrawer?.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onInboxClicked() {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(InboxPage)
            )
            binding?.fragmentListingDrawer?.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onSettingsClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSettingsFragment())
        binding?.fragmentListingDrawer?.closeDrawer(Gravity.LEFT)
    }


//     _____                         __          ___           _
//    |  __ \                        \ \        / (_)         | |
//    | |__) |__  _ __  _   _ _ __    \ \  /\  / / _ _ __   __| | _____      _____
//    |  ___/ _ \| '_ \| | | | '_ \    \ \/  \/ / | | '_ \ / _` |/ _ \ \ /\ / / __|
//    | |  | (_) | |_) | |_| | |_) |    \  /\  /  | | | | | (_| | (_) \ V  V /\__ \
//    |_|   \___/| .__/ \__,_| .__/      \/  \/   |_|_| |_|\__,_|\___/ \_/\_/ |___/
//               | |         | |
//               |_|         |_|

    private fun showPostSortPopup(
        anchor: View,
        currentSort: PostSort,
        onSortSelected: (PostSort) -> Unit
    ) {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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

        popupWindow.showAsDropdown(
            anchor,
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupBinding.root.measuredHeight
        )
    }

    private fun showSearchSortPopup(
        anchor: View,
        currentSort: PostSort,
        onSortSelected: (PostSort) -> Unit
    ) {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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

        popupWindow.showAsDropdown(
            anchor,
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupBinding.root.measuredHeight
        )
    }

    private fun showTimePopup(
        anchor: View,
        currentTimeSort: Time?,
        onTimeSelected: (Time) -> Unit
    ) {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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

        popupWindow.showAsDropdown(
            anchor,
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupBinding.root.measuredHeight
        )
    }

    private fun showMoreOptionsPopup(anchor: View) {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupListingActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            popupListingActionsCreatePost.root.setOnClickListener {
                if ((activity?.application as AstroApplication).accessToken == null) {
                    binding?.fragmentListingDrawer?.let {
                        Snackbar.make(it, R.string.must_be_logged_in, Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    val subredditName = model.subreddit.value?.displayName
                    CreatePostDialogFragment.newInstance(subredditName)
                        .show(parentFragmentManager, null)
                }
                popupWindow.dismiss()
            }
            popupListingActionsSearch.root.setOnClickListener {
                findNavController().navigate(
                    ViewPagerFragmentDirections.actionViewPagerFragmentToSearchFragment(
                        false
                    )
                )
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
        popupWindow.showAsDropdown(
            anchor,
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupBinding.root.measuredHeight
        )
    }

    companion object {
        fun newInstance(postListing: PostListing): PostListingFragment {
            return PostListingFragment().apply {
                arguments = bundleOf(LISTING_KEY to postListing)
            }
        }
    }

}
