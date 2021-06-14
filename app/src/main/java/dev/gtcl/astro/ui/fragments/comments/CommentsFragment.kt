package dev.gtcl.astro.ui.fragments.comments

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.CommentActions
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.FragmentCommentsBinding
import dev.gtcl.astro.databinding.PopupCommentSortBinding
import dev.gtcl.astro.databinding.PopupCommentsPageActionsBinding
import dev.gtcl.astro.html.createHtmlViews
import dev.gtcl.astro.html.toDp
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.manage.ManagePostDialogFragment
import dev.gtcl.astro.ui.fragments.reply_or_edit.ReplyOrEditDialogFragment
import dev.gtcl.astro.ui.fragments.report.ReportDialogFragment
import dev.gtcl.astro.ui.fragments.share.ShareCommentOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.share.SharePostOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.url_menu.FragmentDialogUrlMenu
import dev.gtcl.astro.ui.fragments.view_pager.*

class CommentsFragment : Fragment(), CommentActions, ItemClickListener,
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

    private val movementMethod by lazy {
        createBetterLinkMovementInstance(requireContext(), findNavController(), parentFragmentManager, activityModel)
    }

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
        initOtherObservers()
        initPreviewImage()

        binding?.executePendingBindings()
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
            CommentsAdapter(this, this, userId, model.allCommentsFetched.value == true, movementMethod) {
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
                adapter.removeItemAt(it)
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
            val fullcontextRegex = "(?:(?:http[s]?://)?(?:\\w+\\.)?reddit\\.com/|/)?(r/[A-Za-z0-9_.]+/comments/\\w+(?:/\\w+)?/?)".toRegex()
            val fullContextLink = (fullcontextRegex.find(url) ?: return).value
            if (model.allCommentsFetched.value == true) {
                model.fetchComments(
                    fullContextLink,
                    isFullContext = true,
                    refreshPost = refreshPost
                )
            } else {
                val isFullContext = url.endsWith(fullContextLink, true)
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
                        binding?.fragmentCommentsPostLayout?.invalidateAll()
                        binding?.fragmentCommentsBottomBarLayout?.invalidateAll()
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
                        binding?.fragmentCommentsPostLayout?.invalidateAll()
                        binding?.fragmentCommentsBottomBarLayout?.invalidateAll()
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
                        post.parseSelftext(),
                        null,
                        movementMethod
                    )
                }
                model.contentInitialized = true
            }
        })
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

        model.previewImg.observe(viewLifecycleOwner, {
            loadPreview(it)
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
                    adapter.removeItemAt(position)
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
                    if (item is Post) { // Edit Post's text
                        model.contentInitialized = false
                        model.setPost(item)
                    } else if (item is Comment) { // Edit comment
                        item.depth = if (position >= 0) {
                            (((model.comments.value
                                ?: return@setFragmentResultListener)[position] as Comment).depth
                                ?: 0)
                        } else {
                            0
                        }
                        model.setCommentAt(item, position)
                        adapter.updateItemAt(item, position)
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
    }

    private fun initPreviewImage(){
        binding?.fragmentCommentsContent?.layoutCommentsContentPreviewImage?.apply{
            setOnClickListener {
                val post = model.post.value ?: return@setOnClickListener
                post.urlFormatted?.handleUrl(context, null, post.previewVideoUrl, childFragmentManager, findNavController(), activityModel)
            }
            setOnLongClickListener {
                val url = model.post.value?.urlFormatted ?: return@setOnLongClickListener true
                FragmentDialogUrlMenu.newInstance(url).show(childFragmentManager, null)
                true
            }
        }

        binding?.fragmentCommentsContent?.layoutCommentsContentUrlLayout?.root?.apply {
            setOnClickListener {
                val post = model.post.value ?: return@setOnClickListener
                post.urlFormatted?.handleUrl(context, null, post.previewVideoUrl, childFragmentManager, findNavController(), activityModel)
            }
            setOnLongClickListener {
                val url = model.post.value?.urlFormatted ?: return@setOnLongClickListener true
                FragmentDialogUrlMenu.newInstance(url).show(childFragmentManager, null)
                true
            }
        }
    }

    private fun loadPreview(imgUrl: String?) {
        val imgView = binding?.fragmentCommentsContent?.layoutCommentsContentPreviewImage ?: return
        model.showPreviewIcon(false)
        when{
            imgUrl == null -> imgView.visibility = View.GONE
            !URLUtil.isValidUrl(imgUrl) && !Patterns.WEB_URL.matcher(imgUrl).matches() -> {
                model.showPreviewIcon(true)
                imgView.apply {
                    val size = 256.toDp(context)
                    setImageResource(R.drawable.ic_no_photo_24)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setBackgroundColor(Color.GRAY)
                    layoutParams.height = size
                }
            }
            else -> {
                imgView.apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    scaleType = ImageView.ScaleType.FIT_XY
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                GlideApp.with(imgView.context)
                        .load(imgUrl)
                        .addListener(object: RequestListener<Drawable> {
                            override fun onResourceReady(resource: Drawable?, mod: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                model.showPreviewIcon(true)
                                if(resource is GifDrawable){
                                    imgView.setImageBitmap(resource.firstFrame)
                                    return true
                                }
                                return false
                            }

                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                return false
                            }
                        })
                        .into(imgView)
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
                ReplyOrEditDialogFragment.newInstance(comment, position - adapter.getOffset(), true)
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
            adapter.removeItemAt(position)
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
            ReplyOrEditDialogFragment.newInstance(comment, position - adapter.getOffset(), false)
                .show(childFragmentManager, null)
        }
    }

    override fun delete(comment: Comment, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.delete(comment.name)
            model.removeCommentAt(position)
            adapter.removeItemAt(position)
        }
    }

    override fun itemClicked(item: Item, position: Int) { // "position" is offset by adapter
        val positionWithoutOffset = position - adapter.getOffset()
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
                    model.fetchMoreComments(positionWithoutOffset)
                }
            }
            is Comment -> {
                val collapse = !item.isCollapsed
                item.isCollapsed = collapse
                if(collapse) {
                    item.isExpanded = false
                }
                adapter.notifyItemChanged(position)
                if (collapse) {
                    val hideSize = model.hideResponses(positionWithoutOffset)
                    if (hideSize != 0) {
                        adapter.removeItems(positionWithoutOffset + 1, hideSize)
                    }
                } else {
                    val unhideItems = model.unhideItems(positionWithoutOffset)
                    if (unhideItems.isNotEmpty()) {
                        adapter.addItems(positionWithoutOffset + 1, unhideItems)
                    }
                }
            }
        }
    }

    override fun itemLongClicked(item: Item, position: Int) {
        item.isExpanded = !item.isExpanded
        adapter.notifyItemChanged(position)
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

