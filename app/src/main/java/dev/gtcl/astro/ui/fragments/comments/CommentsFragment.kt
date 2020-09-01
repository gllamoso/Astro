package dev.gtcl.astro.ui.fragments.comments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.R
import dev.gtcl.astro.actions.CommentActions
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.LinkHandler
import dev.gtcl.astro.databinding.FragmentCommentsBinding
import dev.gtcl.astro.databinding.PopupCommentSortBinding
import dev.gtcl.astro.databinding.PopupCommentsPageActionsBinding
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.*
import dev.gtcl.astro.ui.fragments.manage.ManagePostDialogFragment
import dev.gtcl.astro.ui.fragments.media.MediaDialogFragment
import dev.gtcl.astro.ui.fragments.media.list.MediaListAdapter
import dev.gtcl.astro.ui.fragments.media.list.MediaListFragmentAdapter
import dev.gtcl.astro.ui.fragments.share.ShareCommentOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.share.SharePostOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.reply_or_edit.ReplyOrEditDialogFragment
import dev.gtcl.astro.ui.fragments.report.ReportDialogFragment
import io.noties.markwon.*

class CommentsFragment : Fragment(), CommentActions, ItemClickListener, LinkHandler, DrawerLayout.DrawerListener {

    private val model: CommentsVM by lazy {
        val viewModelFactory =
            ViewModelFactory(requireContext().applicationContext as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(CommentsVM::class.java)
    }

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private lateinit var binding: FragmentCommentsBinding

    private lateinit var adapter: CommentsAdapter

    private val markwon: Markwon by lazy {
        createMarkwonInstance(requireContext(), ::handleLink)
    }

    private lateinit var requestPermissionToDownloadItem: ActivityResultLauncher<String>
    private lateinit var requestPermissionToDownloadAlbum: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommentsBinding.inflate(inflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.model = model

        if(!model.commentsFetched){
            initPost()
        }
        initPostObservers()
        initTopBar()
        initBottomBarAndCommentsAdapter()
        initMedia()
        initOtherObservers()

        binding.executePendingBindings()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        model.contentInitialized = false
    }

    private fun initTopBar() {
        binding.fragmentCommentsToolbar.setNavigationOnClickListener {
            viewPagerModel.navigateToPreviousPage()
        }
    }

    private fun initBottomBarAndCommentsAdapter() {
        val fullContextLink = requireArguments().getString(FULL_CONTEXT_URL_KEY, null)
        val onViewAllClick: (() -> Unit)? =
            if(fullContextLink != null){
                {
                    if(model.loading.value != true){
                        model.fetchComments(fullContextLink)
                    }
                }
            } else {
                null
            }
        val userId = (requireActivity().application as AstroApplication).currentAccount?.fullId
        adapter = CommentsAdapter(markwon, this, this, userId, onViewAllClick)
        binding.fragmentCommentsComments.adapter = adapter

        model.allCommentsFetched.observe(viewLifecycleOwner, {
            adapter.allCommentsRetrieved = it
        })

        model.comments.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })

        model.moreComments.observe(viewLifecycleOwner, {
            if (it != null) {
                adapter.addItems(it.position, it.comments)
                model.moreCommentsObserved()
            }
        })

        model.removeAt.observe(viewLifecycleOwner, {
            if(it != null){
                adapter.removeAt(it)
                model.removeAtObserved()
            }
        })

        val behavior = BottomSheetBehavior.from(binding.fragmentCommentsBottomSheet)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(p0: View, p1: Float) {
                viewPagerModel.swipingEnabled(false)
            }

            override fun onStateChanged(p0: View, newState: Int) {
                viewPagerModel.swipingEnabled(newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED)
            }
        })

        binding.fragmentCommentsReply.setOnClickListener {
            val post = model.post.value!!
            if(post.locked || post.deleted){
                Snackbar.make(it, R.string.cannot_reply_to_post, Snackbar.LENGTH_LONG).show()
            } else {
                ReplyOrEditDialogFragment.newInstance(post, -1, true).show(childFragmentManager, null)
            }
        }

        binding.fragmentCommentsSort.setOnClickListener {
            val currentSort = model.commentSort.value!!
            showCommentSortPopup(it, currentSort){ sort ->
                model.setCommentSort(sort)
                model.fetchComments(refreshPost = false)
            }
        }

        initBottomBarOnClickListeners(behavior)
    }

    private fun initBottomBarOnClickListeners(behavior: BottomSheetBehavior<CoordinatorLayout>) {

        binding.fragmentCommentsBottomBarLayout.apply {
            layoutCommentsBottomBarCommentsButton.setOnClickListener {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }

            layoutCommentsBottomBarUpvoteButton.setOnClickListener {
                if (model.loading.value == true) {
                    return@setOnClickListener
                }
                model.post.value?.let {
                    val vote = if (it.likes == true) {
                        Vote.UNVOTE
                    } else {
                        Vote.UPVOTE
                    }
                    it.updateScore(vote)
                    activityModel.vote(it.name, vote)
                    binding.invalidateAll()
                }
            }

            layoutCommentsBottomBarDownvoteButton.setOnClickListener {
                if (model.loading.value == true) {
                    return@setOnClickListener
                }
                model.post.value?.let {
                    val vote = if (it.likes == false) {
                        Vote.UNVOTE
                    } else {
                        Vote.DOWNVOTE
                    }
                    it.updateScore(vote)
                    activityModel.vote(it.name, vote)
                    binding.invalidateAll()
                }
            }

            layoutCommentsBottomBarSaveButton.setOnClickListener {
                if (model.loading.value == true) {
                    return@setOnClickListener
                }
                model.post.value?.let {
                    it.saved = !it.saved
                    activityModel.save(it.name, it.saved)
                    binding.invalidateAll()
                }
            }

            layoutCommentsBottomBarMoreOptions.setOnClickListener {
                showMoreOptions(it)
            }
        }

    }

    private fun initPost() {
        val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
        val url = requireArguments().get(URL_KEY) as String?
        if (postPage != null) {
            model.setPost(postPage.post)
            model.fetchComments(postPage.post.permalink, false)
        } else {
            model.fetchComments(url!!)
            val expand = requireArguments().getBoolean(EXPAND_REPLIES_KEY)
            if(expand){
                BottomSheetBehavior.from(binding.fragmentCommentsBottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun initPostObservers(){
        model.post.observe(viewLifecycleOwner, { post ->
            if(!model.contentInitialized){
                if(post.crosspostParentList != null){
                    binding.fragmentCommentsCrossPostLayout.layoutCrosspostCardView.setOnClickListener {
                        activityModel.newPage(PostPage(post.crosspostParentList[0], -1))
                    }
                }
                if(post.isSelf){
                    markwon.setMarkdown(binding.fragmentCommentsContent.layoutCommentsContentText, post.selftext)
                } else {
                    when(post.url?.getUrlType()){
                        UrlType.OTHER -> initUrlPreview(post.url)
                        else -> model.fetchMediaItems(post)
                    }
                }
                model.contentInitialized = true
            }
        })
    }

    private fun initUrlPreview(url: String){
        binding.fragmentCommentsContent.layoutCommentsContentUrlLayout.root.setOnClickListener {
            handleLink(url)
        }
    }

    private fun initMedia(){
        binding.fragmentCommentsDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        model.mediaItems.observe(viewLifecycleOwner, {
            if(it != null){
                val adapter = MediaListFragmentAdapter(this, it)
                binding.fragmentCommentsContent.layoutCommentsContentViewPager.apply {
                    this.adapter = adapter
                    (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                    if(it.size > 1){
                        registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                            override fun onPageScrollStateChanged(state: Int) {
                                super.onPageScrollStateChanged(state)
                                if(state == ViewPager2.SCROLL_STATE_IDLE){
                                    binding.fragmentCommentsContent.apply {
                                        layoutCommentsContentPreviousButton.visibility = if(currentItem == 0) View.GONE else View.VISIBLE
                                        layoutCommentsContentNextButton.visibility = if(currentItem == it.size - 1) View.GONE else View.VISIBLE
                                    }
                                }
                            }
                        })

                        binding.fragmentCommentsContent.apply {
                            layoutCommentsContentPreviousButton.visibility = if(currentItem == 0) View.GONE else View.VISIBLE
                            layoutCommentsContentNextButton.visibility = if(currentItem == it.size - 1) View.GONE else View.VISIBLE
                        }
                    }
                }

                if(it.size > 1){

                    binding.fragmentCommentsDrawer.apply {
                        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                        addDrawerListener(this@CommentsFragment)
                    }

                    val mediaListAdapter =
                        MediaListAdapter { position ->
                            binding.fragmentCommentsContent.layoutCommentsContentViewPager.currentItem = position
                            binding.fragmentCommentsDrawer.closeDrawer(GravityCompat.END)
                        }
                    mediaListAdapter.submitList(it)

                    binding.apply {
                        fragmentCommentsThumbnailsList.adapter = mediaListAdapter
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
        })
    }

    private fun initOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, {
            if(it != null){
                Snackbar.make(binding.fragmentCommentsBottomBar, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        binding.fragmentCommentsSwipeRefresh.setOnRefreshListener {
            if(model.loading.value == true){
                return@setOnRefreshListener
            }

            model.contentInitialized = false
            val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
            if(postPage != null){
                model.fetchComments()
            } else {
                val url = requireArguments().getString(URL_KEY)
                val fullContextLink = requireArguments().getString(FULL_CONTEXT_URL_KEY, null)
                if(model.allCommentsFetched.value == true){
                    if(fullContextLink != null){
                        model.fetchComments(fullContextLink)
                    } else {
                        model.fetchComments(url!!)
                    }
                } else {
                    model.fetchComments(url!!)
                }
            }
        }

        model.loading.observe(viewLifecycleOwner, {
            if(it == false){
                binding.fragmentCommentsSwipeRefresh.isRefreshing = false
            }
        })

        childFragmentManager.setFragmentResultListener(REPORT_KEY, viewLifecycleOwner, { _, bundle ->
            val  position = bundle.getInt(POSITION_KEY, -1)
            if(position == -1 && model.post.value != null){
                model.post.value!!.hidden = true
                binding.fragmentCommentsPostLayout.invalidateAll()
            }
        })

        childFragmentManager.setFragmentResultListener(NEW_REPLY_KEY, viewLifecycleOwner,
            { _, bundle ->
                val item = bundle.get(ITEM_KEY) as Item
                val position = bundle.getInt(POSITION_KEY)
                val reply = bundle.getBoolean(NEW_REPLY_KEY)
                if(reply && item is Comment){
                    item.depth = if(position >= 0){
                        ((model.comments.value!![position] as Comment).depth ?: 0) + 1
                    } else {
                        0
                    }
                    model.addItems(position + 1, listOf(item))
                    adapter.addItems(position + 1, listOf(item))
                } else if(!reply) {
                    if(item is Post){
                        model.contentInitialized = false
                        model.setPost(item)
                    } else if(item is Comment){
                        item.depth = if(position >= 0){
                            ((model.comments.value!![position] as Comment).depth ?: 0)
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
                if(model.post.value != null){
                    val nsfw = bundle.getBoolean(NSFW_KEY)
                    val spoiler = bundle.getBoolean(SPOILER_KEY)
                    val getNotification = bundle.getBoolean(GET_NOTIFICATIONS_KEY)
                    val flair = bundle.get(FLAIRS_KEY) as Flair?
                    val post = model.post.value!!
                    activityModel.updatePost(post, nsfw, spoiler, getNotification, flair)
                }
            })


        requestPermissionToDownloadItem = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                model.downloadItem(binding.fragmentCommentsContent.layoutCommentsContentViewPager.currentItem)
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_grant_necessary_permissions), Toast.LENGTH_LONG).show()
            }
        }

        requestPermissionToDownloadAlbum = registerForActivityResult(ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                model.downloadAlbum()
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_grant_necessary_permissions), Toast.LENGTH_LONG).show()
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
        comment.updateScore(vote)
        activityModel.vote(comment.name, vote)
    }

    override fun save(comment: Comment) {
        activityModel.save(comment.name, comment.saved == true)
    }

    override fun share(comment: Comment) {
        ShareCommentOptionsDialogFragment.newInstance(comment).show(parentFragmentManager, null)
    }

    override fun reply(comment: Comment, position: Int) {
        if(comment.locked == true || comment.deleted){
            Snackbar.make(binding.fragmentCommentsReply, R.string.cannot_reply_to_comment, Snackbar.LENGTH_LONG).show()
        } else {
            ReplyOrEditDialogFragment.newInstance(comment, position, true).show(childFragmentManager, null)
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
        ReportDialogFragment.newInstance(comment, position).show(childFragmentManager, null)
    }

    override fun edit(comment: Comment, position: Int) {
        ReplyOrEditDialogFragment.newInstance(comment, position, false).show(childFragmentManager, null)
    }

    override fun delete(comment: Comment, position: Int) {
        activityModel.delete(comment.name)
        model.removeCommentAt(position)
        adapter.removeAt(position)
    }

    override fun itemClicked(item: Item, position: Int) {
        when(item) {
            is More -> {
                if (item.isContinueThreadLink) {
                    activityModel.newPage(ContinueThreadPage("${model.post.value!!.permalink}${item.parentId.replace("t1_", "")}", model.post.value?.permalink, true))
                } else {
                    model.fetchMoreComments(position)
                }
            }
            is Comment -> {
                val collapse = !item.isCollapsed
                item.isCollapsed = collapse
                adapter.notifyItemChanged(position)
                if(collapse){
                    val hideSize = model.hideItems(position)
                    if(hideSize != 0){
                        adapter.removeRange(position + 1, hideSize)
                    }
                } else {
                    val unhideItems = model.unhideItems(position)
                    if(unhideItems.isNotEmpty()){
                        adapter.addItems(position + 1, unhideItems)
                    }
                }
            }
        }
    }

    override fun handleLink(link: String) {
        when(link.getUrlType()){
            UrlType.IMAGE -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.PICTURE)).show(childFragmentManager, null)
            UrlType.GIF -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.GIF)).show(childFragmentManager, null)
            UrlType.GIFV, UrlType.HLS, UrlType.STANDARD_VIDEO -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.VIDEO)).show(childFragmentManager, null)
            UrlType.GFYCAT -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.GFYCAT)).show(childFragmentManager, null)
            UrlType.REDGIFS -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.REDGIFS)).show(childFragmentManager, null)
            UrlType.IMGUR_ALBUM -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.IMGUR_ALBUM)).show(childFragmentManager, null)
            UrlType.REDDIT_COMMENTS -> activityModel.newPage(ContinueThreadPage(link, null, true))
            UrlType.OTHER, UrlType.REDDIT_VIDEO -> activityModel.openChromeTab(link)
            UrlType.IMGUR_IMAGE -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.IMGUR_PICTURE)).show(childFragmentManager, null)
            null -> throw IllegalArgumentException("Unable to determine link type: $link")
        }
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

    private fun showCommentSortPopup(anchor: View, commentSort: CommentSort, onSortSelected: (CommentSort) -> Unit){

        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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

        popupWindow.showAsDropdown(anchor, popupBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, popupBinding.root.measuredHeight)
    }

    private fun showMoreOptions(anchor: View){
        if(model.post.value == null){
            return
        }

        val loggedIn = (requireActivity().application as AstroApplication).currentAccount != null

        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupCommentsPageActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            val post = model.post.value!!
            val currentAccount = (this@CommentsFragment.requireActivity().application as AstroApplication).currentAccount
            val currentMediaUrl = model.mediaItems.value?.get(binding.fragmentCommentsContent.layoutCommentsContentViewPager.currentItem)
            val currentMediaType = when(currentMediaUrl?.mediaType){
                MediaType.GIF, MediaType.PICTURE -> SimpleMediaType.PICTURE
                MediaType.VIDEO -> SimpleMediaType.VIDEO
                else -> null
            }
            this.post = post
            this.createdFromUser = currentAccount != null && currentAccount.fullId == post.authorFullName
            this.currentItemMediaType = currentMediaType
            if(createdFromUser == true){
                if(post.isSelf){
                    popupCommentsPageActionsEdit.root.setOnClickListener {
                        ReplyOrEditDialogFragment.newInstance(post, -1, false).show(childFragmentManager, null)
                        popupWindow.dismiss()
                    }
                }
                popupCommentsPageActionsManage.root.setOnClickListener {
                    ManagePostDialogFragment.newInstance(post).show(childFragmentManager, null)
                    popupWindow.dismiss()
                }
                popupCommentsPageActionsDelete.root.setOnClickListener {
                    activityModel.delete(post.name)
                    popupWindow.dismiss()
                }
            }
            popupCommentsPageActionsProfile.root.setOnClickListener {
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(post.author)))
                popupWindow.dismiss()
            }
            popupCommentsPageActionsSubreddits.root.setOnClickListener {
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(ListingPage(SubredditListing(post.subreddit))))
                popupWindow.dismiss()
            }
            popupCommentsPageActionsHide.root.setOnClickListener {
                if (!loggedIn) {
                    Snackbar.make(binding.root, R.string.must_be_logged_in, Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                post.hidden = !post.hidden
                binding.invalidateAll()
                activityModel.hide(post.name, post.hidden)
                popupWindow.dismiss()
            }
            popupCommentsPageActionsShare.root.setOnClickListener {
                SharePostOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
                popupWindow.dismiss()
            }
            if(currentItemMediaType != null){
                popupCommentsPageActionsFullScreen.root.setOnClickListener {
                    MediaDialogFragment.newInstance(post.url!!, model.mediaItems.value!!).show(childFragmentManager, null)
                    popupWindow.dismiss()
                }
                popupCommentsPageActionsDownloadSingleItem.root.setOnClickListener {
                    if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                        model.downloadItem(binding.fragmentCommentsContent.layoutCommentsContentViewPager.currentItem)
                    } else {
                        requestPermissionToDownloadItem.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    popupWindow.dismiss()
                }
                if(post.urlType == UrlType.IMGUR_ALBUM){
                    popupCommentsPageActionsDownloadAll.root.setOnClickListener {
                        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                            model.downloadAlbum()
                        } else {
                            requestPermissionToDownloadAlbum.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        popupWindow.dismiss()
                    }
                }
            }
            if(post.url != null){
                popupCommentsPageActionsLink.root.setOnClickListener {
                    activityModel.openChromeTab(post.url)
                    popupWindow.dismiss()
                }
            }
            popupCommentsPageActionsReport.root.setOnClickListener {
                if (!loggedIn) {
                    Snackbar.make(binding.root, R.string.must_be_logged_in, Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                ReportDialogFragment.newInstance(post).show(childFragmentManager, null)
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
        fun newInstance(postPage: PostPage): CommentsFragment {
            val fragment = CommentsFragment()
            val args = bundleOf(POST_PAGE_KEY to postPage)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(url: String, fullContextLink: String?, expandReplies: Boolean): CommentsFragment {
            val fragment = CommentsFragment()
            val args = bundleOf(URL_KEY to url, FULL_CONTEXT_URL_KEY to fullContextLink, EXPAND_REPLIES_KEY to expandReplies)
            fragment.arguments = args
            return fragment
        }
    }
}

