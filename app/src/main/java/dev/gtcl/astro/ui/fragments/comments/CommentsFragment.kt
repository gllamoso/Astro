package dev.gtcl.astro.ui.fragments.comments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.CommentActions
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.LinkHandler
import dev.gtcl.astro.databinding.FragmentCommentsBinding
import dev.gtcl.astro.databinding.PopupCommentSortBinding
import dev.gtcl.astro.databinding.PopupCommentsPageActionsBinding
import dev.gtcl.astro.html.createHtmlViews
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.manage.ManagePostDialogFragment
import dev.gtcl.astro.ui.fragments.media.list.MediaListFragmentAdapter
import dev.gtcl.astro.ui.fragments.media.list.MediaThumbnailsAdapter
import dev.gtcl.astro.ui.fragments.reply_or_edit.ReplyOrEditDialogFragment
import dev.gtcl.astro.ui.fragments.report.ReportDialogFragment
import dev.gtcl.astro.ui.fragments.share.ShareCommentOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.share.SharePostOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.view_pager.*

class CommentsFragment : Fragment(), CommentActions, ItemClickListener, LinkHandler,
    DrawerLayout.DrawerListener {

    private val model: CommentsVM by lazy {
        val viewModelFactory =
            ViewModelFactory(requireContext().applicationContext as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(CommentsVM::class.java)
    }

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private var binding: FragmentCommentsBinding? = null

    private lateinit var adapter: CommentsAdapter

    private lateinit var requestPermissionToDownloadItem: ActivityResultLauncher<String>
    private lateinit var requestPermissionToDownloadAlbum: ActivityResultLauncher<String>

    override fun onResume() {
        super.onResume()
        viewPagerModel.syncViewPager()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommentsBinding.inflate(inflater)
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.model = model
        activityModel.showMediaControls(true)

        if (model.post.value == null && model.loading.value != true) {
            val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
            if (postPage != null) {
                model.setPost(postPage.post)
            }
            refresh(true)
        }
        initPostObservers()
        initTopBar()
        initBottomBarAndBottomSheet()
        initMedia()
        initOtherObservers()

        binding?.executePendingBindings()
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.fragmentCommentsContent?.layoutCommentsContentViewPager?.adapter = null
        model.contentInitialized = false
        model.viewPagerInitialized = false
        binding = null
    }

    private fun initTopBar() {
        binding?.fragmentCommentsToolbar?.setNavigationOnClickListener {
            viewPagerModel.navigateToPreviousPage()
        }
    }

    private fun initBottomBarAndBottomSheet() {

        val userId = (requireActivity().application as AstroApplication).currentAccount?.fullId
        adapter =
            CommentsAdapter(this, this, userId, model.allCommentsFetched.value == true, this) {
                if (model.loading.value != true) {
                    model.fetchFullContext()
                }
            }
        binding?.fragmentCommentsComments?.adapter = adapter

        model.allCommentsFetched.observe(viewLifecycleOwner, {
            adapter.allCommentsFetched = it
        })

        model.comments.observe(viewLifecycleOwner, {
            if (it != null) {
                adapter.submitList(it)
            }
        })

        model.moreComments.observe(viewLifecycleOwner, {
            if (it != null) {
                adapter.addItems(it.position, it.comments)
                model.moreCommentsObserved()
            }
        })

        model.removeAt.observe(viewLifecycleOwner, {
            if (it != null) {
                adapter.removeAt(it)
                model.removeAtObserved()
            }
        })

        model.notifyAt.observe(viewLifecycleOwner, {
            if (it != null) {
                adapter.notifyItemChanged(it)
                model.notifyAtObserved()
            }
        })
        val behavior = BottomSheetBehavior.from((binding ?: return).fragmentCommentsBottomSheet)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(p0: View, p1: Float) {}

            override fun onStateChanged(p0: View, newState: Int) {
                viewPagerModel.swipingEnabled(
                    newState == BottomSheetBehavior.STATE_HIDDEN ||
                            newState == BottomSheetBehavior.STATE_COLLAPSED ||
                            newState == BottomSheetBehavior.STATE_EXPANDED
                )
                model.commentsExpanded = newState == BottomSheetBehavior.STATE_EXPANDED
            }
        })

        val expand = when {
            model.commentsExpanded != null -> model.commentsExpanded ?: return
            else -> requireArguments().getBoolean(EXPAND_REPLIES_KEY, false)
        }

        if (expand) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding?.fragmentCommentsRefresh?.setOnClickListener {
            model.clearComments()
            refresh(false)
        }

        binding?.fragmentCommentsReply?.setOnClickListener {
            checkIfLoggedInBeforeExecuting(requireContext()) {
                val post = model.post.value ?: return@checkIfLoggedInBeforeExecuting
                if (post.locked || post.deleted) {
                    Snackbar.make(it, R.string.cannot_reply_to_post, Snackbar.LENGTH_LONG).show()
                } else {
                    ReplyOrEditDialogFragment.newInstance(post, -1, true)
                        .show(childFragmentManager, null)
                }
            }
        }

        binding?.fragmentCommentsSort?.setOnClickListener {
            val currentSort = model.commentSort.value ?: return@setOnClickListener
            showCommentSortPopup(it, currentSort) { sort ->
                model.setCommentSort(sort)
                refresh(false)
            }
        }

        initBottomBarOnClickListeners(behavior)
    }

    private fun refresh(refreshPost: Boolean) {
        model.contentInitialized = false
        val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
        if (postPage != null) {
            model.setAllCommentsFetched(true)
            model.fetchComments(
                postPage.post.permalinkFormatted,
                isFullContext = true,
                refreshPost = refreshPost
            )
        } else {
            val url = requireArguments().getString(URL_KEY) ?: return
            val fullContextLink = (VALID_REDDIT_COMMENTS_URL_REGEX.find(url) ?: return).value
            if (model.allCommentsFetched.value == true) {
                model.fetchComments(
                    fullContextLink,
                    isFullContext = true,
                    refreshPost = refreshPost
                )
            } else {
                val isFullContext = url == fullContextLink
                if(isFullContext){
                    model.setAllCommentsFetched(true)
                }
                model.fetchComments(url, isFullContext = isFullContext, refreshPost = refreshPost)
            }
        }
    }

    private fun initBottomBarOnClickListeners(behavior: BottomSheetBehavior<out ViewGroup>) {

        binding?.fragmentCommentsBottomBarLayout?.apply {
            layoutCommentsBottomBarCommentsButton.setOnClickListener {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            layoutCommentsBottomBarUpvoteButton.setOnClickListener {
                checkIfLoggedInBeforeExecuting(requireContext()) {
                    if (model.loading.value == true) {
                        return@checkIfLoggedInBeforeExecuting
                    }
                    model.post.value?.let {
                        val vote = if (it.likes == true) {
                            Vote.UNVOTE
                        } else {
                            Vote.UPVOTE
                        }
                        it.updateScore(vote)
                        activityModel.vote(it.name, vote)
                        binding?.invalidateAll()
                    }
                }
            }

            layoutCommentsBottomBarDownvoteButton.setOnClickListener {
                checkIfLoggedInBeforeExecuting(requireContext()) {
                    if (model.loading.value == true) {
                        return@checkIfLoggedInBeforeExecuting
                    }
                    model.post.value?.let {
                        val vote = if (it.likes == false) {
                            Vote.UNVOTE
                        } else {
                            Vote.DOWNVOTE
                        }
                        it.updateScore(vote)
                        activityModel.vote(it.name, vote)
                        binding?.invalidateAll()
                    }
                }
            }

            layoutCommentsBottomBarSaveButton.setOnClickListener {
                checkIfLoggedInBeforeExecuting(requireContext()) {
                    if (model.loading.value == true) {
                        return@checkIfLoggedInBeforeExecuting
                    }
                    model.post.value?.let {
                        it.saved = !it.saved
                        activityModel.save(it.name, it.saved)
                        binding?.invalidateAll()
                    }
                }
            }

            layoutCommentsBottomBarMoreOptions.setOnClickListener {
                showMoreOptions(it)
            }
        }

    }

    private fun initPostObservers() {
        model.post.observe(viewLifecycleOwner, { post ->
            if (!model.contentInitialized) {
                if (post.crosspostParentList != null) {
                    binding?.fragmentCommentsCrossPostLayout?.layoutCrosspostCardView?.setOnClickListener {
                        activityModel.newViewPagerPage(PostPage(post.crosspostParentList[0], -1))
                    }
                }
                if (post.isSelf) {
                    binding?.fragmentCommentsContent?.layoutCommentsContentTextLayout?.createHtmlViews(
                        post.parseSelfText(),
                            null,
                        this
                    )
                } else {
                    when (post.urlType) {
                        UrlType.OTHER, UrlType.REDDIT_COMMENTS, UrlType.REDDIT_THREAD -> initUrlPreview(
                            post.urlFormatted ?: return@observe
                        )
                        else -> model.fetchMediaItems(post)
                    }
                }
                model.contentInitialized = true
            }
        })
    }

    private fun initUrlPreview(url: String) {
        binding?.fragmentCommentsContent?.layoutCommentsContentUrlLayout?.layoutUrlWithThumbnailCardView?.setOnClickListener {
            handleLink(url)
        }
    }

    private fun initMedia() {
        binding?.fragmentCommentsDrawer?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        model.mediaItems.observe(viewLifecycleOwner, {
            if (it?.size ?: 0 > 0 && !model.viewPagerInitialized) {
                setMediaInViewPager(it ?: return@observe)
                model.viewPagerInitialized = true
            }
        })
    }

    private fun setMediaInViewPager(mediaUrls: List<MediaURL>) {
        val mediaAdapter = MediaListFragmentAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle,
            mediaUrls,
            false
        )

        val currentPosition =
            binding?.fragmentCommentsContent?.layoutCommentsContentViewPager?.currentItem ?: 0
        val mediaThumbnails =
            MediaThumbnailsAdapter(currentPosition) { position ->
                binding?.fragmentCommentsContent?.layoutCommentsContentViewPager?.currentItem =
                    position
                binding?.fragmentCommentsDrawer?.closeDrawer(GravityCompat.END)
                val behavior = BottomSheetBehavior.from(
                    ((binding ?: return@MediaThumbnailsAdapter).fragmentCommentsBottomSheet)
                )
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }

        binding?.fragmentCommentsContent?.layoutCommentsContentViewPager?.apply {
            this.adapter = mediaAdapter
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            if (mediaUrls.size > 1) {
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        binding?.fragmentCommentsContent?.apply {
                            layoutCommentsContentPreviousButton.visibility =
                                if (position == 0) View.GONE else View.VISIBLE
                            layoutCommentsContentNextButton.visibility =
                                if (position == mediaUrls.size - 1) View.GONE else View.VISIBLE
                        }
                        if (mediaUrls.size > 1) {
                            mediaThumbnails.setCurrentPosition(position)
                        }
                    }
                })

                binding?.fragmentCommentsContent?.apply {
                    layoutCommentsContentPreviousButton.visibility =
                        if (currentItem == 0) View.GONE else View.VISIBLE
                    layoutCommentsContentNextButton.visibility =
                        if (currentItem == mediaUrls.size - 1) View.GONE else View.VISIBLE
                }
            }
        }

        if (mediaUrls.size > 1) {

            binding?.fragmentCommentsDrawer?.apply {
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                addDrawerListener(this@CommentsFragment)
            }

            mediaThumbnails.submitList(mediaUrls)

            binding?.apply {
                fragmentCommentsThumbnailsList.adapter = mediaThumbnails
                fragmentCommentsThumbnailsIcon.setOnClickListener {
                    fragmentCommentsDrawer.openDrawer(GravityCompat.END)
                }
                fragmentCommentsContent.layoutCommentsContentPreviousButton.setOnClickListener {
                    fragmentCommentsContent.layoutCommentsContentViewPager.currentItem -= 1
                }
                fragmentCommentsContent.layoutCommentsContentNextButton.setOnClickListener {
                    fragmentCommentsContent.layoutCommentsContentViewPager.currentItem += 1
                }
            }

        }

    }

    private fun initOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                context?.let {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                model.errorMessageObserved()
            }
        })

        binding?.fragmentCommentsSwipeRefresh?.setOnRefreshListener {
            if (model.loading.value == true) {
                binding?.fragmentCommentsSwipeRefresh?.isRefreshing = false
                return@setOnRefreshListener
            }
            model.viewPagerInitialized = false
            refresh(true)
        }

        model.loading.observe(viewLifecycleOwner, {
            if (it == false) {
                binding?.fragmentCommentsSwipeRefresh?.isRefreshing = false
            }
        })

        childFragmentManager.setFragmentResultListener(
            REPORT_KEY,
            viewLifecycleOwner,
            { _, bundle ->
                val position = bundle.getInt(POSITION_KEY, -1)
                if (position == -1 && model.post.value != null) {
                    (model.post.value ?: return@setFragmentResultListener).hidden = true
                    binding?.fragmentCommentsPostLayout?.invalidateAll()
                } else {
                    adapter.removeAt(position)
                    model.removeCommentAt(position)
                }
            })

        childFragmentManager.setFragmentResultListener(NEW_REPLY_KEY, viewLifecycleOwner,
            { _, bundle ->
                val item = bundle.get(ITEM_KEY) as Item
                val position = bundle.getInt(POSITION_KEY)
                val reply = bundle.getBoolean(NEW_REPLY_KEY)
                if (reply && item is Comment) {
                    item.depth = if (position >= 0) {
                        (((model.comments.value
                            ?: return@setFragmentResultListener)[position] as Comment).depth
                            ?: 0) + 1
                    } else {
                        0
                    }
                    model.addItems(position + 1, listOf(item))
                    adapter.addItems(position + 1, listOf(item))
                } else if (!reply) {
                    if (item is Post) {
                        model.contentInitialized = false
                        model.setPost(item)
                    } else if (item is Comment) {
                        item.depth = if (position >= 0) {
                            (((model.comments.value
                                ?: return@setFragmentResultListener)[position] as Comment).depth
                                ?: 0)
                        } else {
                            0
                        }
                        model.setCommentAt(item, position)
                        adapter.updateAt(item, position)
                    }
                }
            })

        childFragmentManager.setFragmentResultListener(MANAGE_POST_KEY, viewLifecycleOwner,
            { _, bundle ->
                if (model.post.value != null) {
                    val nsfw = bundle.getBoolean(NSFW_KEY)
                    val spoiler = bundle.getBoolean(SPOILER_KEY)
                    val getNotification = bundle.getBoolean(GET_NOTIFICATIONS_KEY)
                    val flair = bundle.get(FLAIRS_KEY) as Flair?
                    val post = model.post.value ?: return@setFragmentResultListener
                    activityModel.updatePost(post, nsfw, spoiler, getNotification, flair)
                }
            })


        requestPermissionToDownloadItem = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                model.downloadItem(
                    binding?.fragmentCommentsContent?.layoutCommentsContentViewPager?.currentItem
                        ?: 0
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_grant_necessary_permissions),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        requestPermissionToDownloadAlbum = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                model.downloadAlbum()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.please_grant_necessary_permissions),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

//      _____                                     _                  _   _
//     / ____|                                   | |       /\       | | (_)
//    | |     ___  _ __ ___  _ __ ___   ___ _ __ | |_     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___| (_) | | | | | | | | | | |  __/ | | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    \_____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/

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
                ReplyOrEditDialogFragment.newInstance(comment, position, true)
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
            adapter.removeAt(position)
            model.removeCommentAt(position)
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
            model.removeCommentAt(position)
            adapter.removeAt(position)
        }
    }

    override fun itemClicked(item: Item, position: Int) {
        when (item) {
            is More -> {
                if (item.isContinueThreadLink) {
                    activityModel.newViewPagerPage(
                        CommentsPage(
                            "${(model.post.value ?: return).permalinkWithRedditDomain}${
                                item.parentId.replace(
                                    "t1_",
                                    ""
                                )
                            }", true
                        )
                    )
                } else {
                    model.fetchMoreComments(position)
                }
            }
            is Comment -> {
                val collapse = !item.isCollapsed
                item.isCollapsed = collapse
                adapter.notifyItemChanged(position)
                if (collapse) {
                    val hideSize = model.hideItems(position)
                    if (hideSize != 0) {
                        adapter.removeRange(position + 1, hideSize)
                    }
                } else {
                    val unhideItems = model.unhideItems(position)
                    if (unhideItems.isNotEmpty()) {
                        adapter.addItems(position + 1, unhideItems)
                    }
                }
            }
        }
    }

    override fun handleLink(link: String) {
        viewPagerModel.linkClicked(link)
    }

//     _____                                             _   _
//    |  __ \                                  /\       | | (_)
//    | |  | |_ __ __ ___      _____ _ __     /  \   ___| |_ _  ___  _ __  ___
//    | |  | | '__/ _` \ \ /\ / / _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |__| | | | (_| |\ V  V /  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/|_|  \__,_| \_/\_/ \___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/
//


    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

    override fun onDrawerOpened(drawerView: View) {
        viewPagerModel.swipingEnabled(false)
    }

    override fun onDrawerClosed(drawerView: View) {
        viewPagerModel.swipingEnabled(true)
    }

    override fun onDrawerStateChanged(newState: Int) {}

//     _____                         __          ___           _
//    |  __ \                        \ \        / (_)         | |
//    | |__) |__  _ __  _   _ _ __    \ \  /\  / / _ _ __   __| | _____      _____
//    |  ___/ _ \| '_ \| | | | '_ \    \ \/  \/ / | | '_ \ / _` |/ _ \ \ /\ / / __|
//    | |  | (_) | |_) | |_| | |_) |    \  /\  /  | | | | | (_| | (_) \ V  V /\__ \
//    |_|   \___/| .__/ \__,_| .__/      \/  \/   |_|_| |_|\__,_|\___/ \_/\_/ |___/
//               | |         | |
//               |_|         |_|

    private fun showCommentSortPopup(
        anchor: View,
        commentSort: CommentSort,
        onSortSelected: (CommentSort) -> Unit
    ) {

        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupCommentSortBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            sort = commentSort
            popupCommentSortBest.root.setOnClickListener {
                onSortSelected(CommentSort.BEST)
                popupWindow.dismiss()
            }
            popupCommentSortTop.root.setOnClickListener {
                onSortSelected(CommentSort.TOP)
                popupWindow.dismiss()
            }
            popupCommentSortNew.root.setOnClickListener {
                onSortSelected(CommentSort.NEW)
                popupWindow.dismiss()
            }
            popupCommentSortControversial.root.setOnClickListener {
                onSortSelected(CommentSort.CONTROVERSIAL)
                popupWindow.dismiss()
            }
            popupCommentSortOld.root.setOnClickListener {
                onSortSelected(CommentSort.OLD)
                popupWindow.dismiss()
            }
            popupCommentSortRandom.root.setOnClickListener {
                onSortSelected(CommentSort.RANDOM)
                popupWindow.dismiss()
            }
            popupCommentSortQa.root.setOnClickListener {
                onSortSelected(CommentSort.QA)
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

    private fun showMoreOptions(anchor: View) {
        if (model.post.value == null) {
            return
        }

        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupCommentsPageActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            val viewModel = this@CommentsFragment.model
            this.model = viewModel
            val post = viewModel.post.value ?: return@apply
            val currentMediaUrl = viewModel.mediaItems.value?.get(
                binding?.fragmentCommentsContent?.layoutCommentsContentViewPager?.currentItem ?: 0
            )
            val currentMediaType = when (currentMediaUrl?.mediaType) {
                MediaType.GIF, MediaType.PICTURE -> SimpleMediaType.PICTURE
                MediaType.VIDEO -> SimpleMediaType.VIDEO
                else -> null
            }
            this.currentItemMediaType = currentMediaType
            if (viewModel.postCreatedFromUser) {
                if (post.isSelf) {
                    popupCommentsPageActionsEdit.root.setOnClickListener {
                        checkIfLoggedInBeforeExecuting(requireContext()) {
                            ReplyOrEditDialogFragment.newInstance(post, -1, false)
                                .show(childFragmentManager, null)
                        }
                        popupWindow.dismiss()
                    }
                }
                popupCommentsPageActionsManage.root.setOnClickListener {
                    checkIfLoggedInBeforeExecuting(requireContext()) {
                        ManagePostDialogFragment.newInstance(post).show(childFragmentManager, null)
                    }
                    popupWindow.dismiss()
                }
                popupCommentsPageActionsDelete.root.setOnClickListener {
                    checkIfLoggedInBeforeExecuting(requireContext()) {
                        post.author = "[deleted]"
                        binding?.invalidateAll()
                        activityModel.delete(post.name)
                    }
                    popupWindow.dismiss()
                }
            }
            popupCommentsPageActionsProfile.root.setOnClickListener {
                findNavController().navigate(
                    ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                        AccountPage(post.author)
                    )
                )
                popupWindow.dismiss()
            }
            popupCommentsPageActionsSubreddits.root.setOnClickListener {
                findNavController().navigate(
                    ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                        ListingPage(SubredditListing(post.subreddit))
                    )
                )
                popupWindow.dismiss()
            }
            popupCommentsPageActionsHide.root.setOnClickListener {
                checkIfLoggedInBeforeExecuting(requireContext()) {
                    post.hidden = !post.hidden
                    binding?.invalidateAll()
                    activityModel.hide(post.name, post.hidden)
                }
                popupWindow.dismiss()
            }
            popupCommentsPageActionsShare.root.setOnClickListener {
                SharePostOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
                popupWindow.dismiss()
            }
            if (currentItemMediaType != null) {
                popupCommentsPageActionsDownloadSingleItem.root.setOnClickListener {
                    val permissionGranted = ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    if (permissionGranted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        viewModel.downloadItem(
                            binding?.fragmentCommentsContent?.layoutCommentsContentViewPager?.currentItem
                                ?: 0
                        )
                    } else {
                        requestPermissionToDownloadItem.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    popupWindow.dismiss()
                }
                if (viewModel.mediaItems.value?.size ?: 0 > 1) {
                    popupCommentsPageActionsDownloadAll.root.setOnClickListener {
                        val permissionGranted = ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                        if (permissionGranted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            viewModel.downloadAlbum()
                        } else {
                            requestPermissionToDownloadAlbum.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        popupWindow.dismiss()
                    }
                }
            }
            if (post.urlFormatted != null) {
                popupCommentsPageActionsLink.root.setOnClickListener {
                    activityModel.openChromeTab(post.urlFormatted)
                    popupWindow.dismiss()
                }
            }
            popupCommentsPageActionsReport.root.setOnClickListener {
                checkIfLoggedInBeforeExecuting(requireContext()) {
                    ReportDialogFragment.newInstance(post).show(childFragmentManager, null)
                }
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
        fun newInstance(postPage: PostPage): CommentsFragment {
            val fragment = CommentsFragment()
            val args = bundleOf(POST_PAGE_KEY to postPage)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(url: String, expandReplies: Boolean): CommentsFragment {
            val fragment = CommentsFragment()
            val args = bundleOf(URL_KEY to url, EXPAND_REPLIES_KEY to expandReplies)
            fragment.arguments = args
            return fragment
        }
    }
}

