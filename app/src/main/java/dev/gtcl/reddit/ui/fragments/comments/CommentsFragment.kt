package dev.gtcl.reddit.ui.fragments.comments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RelativeLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.CommentActions
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.LinkHandler
import dev.gtcl.reddit.databinding.FragmentCommentsBinding
import dev.gtcl.reddit.databinding.PopupCommentSortBinding
import dev.gtcl.reddit.databinding.PopupCommentsPageOptionsBinding
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.*
import dev.gtcl.reddit.ui.fragments.media.MediaDialogFragment
import dev.gtcl.reddit.ui.fragments.media.list.MediaListAdapter
import dev.gtcl.reddit.ui.fragments.media.list.MediaListFragmentAdapter
import dev.gtcl.reddit.ui.fragments.misc.ShareCommentOptionsDialogFragment
import dev.gtcl.reddit.ui.fragments.misc.SharePostOptionsDialogFragment
import dev.gtcl.reddit.ui.fragments.reply.ReplyDialogFragment
import dev.gtcl.reddit.ui.fragments.reply.ReplyVM
import dev.gtcl.reddit.ui.fragments.report.ReportDialogFragment
import io.noties.markwon.*

class CommentsFragment : Fragment(), CommentActions, ItemClickListener, LinkHandler, DrawerLayout.DrawerListener {

    private val model: CommentsVM by lazy {
        val viewModelFactory =
            ViewModelFactory(requireContext().applicationContext as RedditApplication)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommentsBinding.inflate(inflater)
        binding.lifecycleOwner = this
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
        binding.toolbar.setNavigationOnClickListener {
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
        adapter = CommentsAdapter(markwon, this, this, onViewAllClick)
        binding.commentList.adapter = adapter

        model.allCommentsFetched.observe(viewLifecycleOwner, Observer {
            adapter.allCommentsRetrieved = it
        })

        model.comments.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

        model.moreComments.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.addItems(it.position, it.comments)
                model.moreCommentsObserved()
            }
        })

        model.removeAt.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.removeAt(it)
                model.removeAtObserved()
            }
        })

        val behavior = BottomSheetBehavior.from(binding.bottomSheet)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(p0: View, p1: Float) {
                viewPagerModel.swipingEnabled(false)
            }

            override fun onStateChanged(p0: View, newState: Int) {
                viewPagerModel.swipingEnabled(newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED)
            }
        })

        binding.replyButton.setOnClickListener {
            ReplyDialogFragment.newInstance(model.post.value!!, 0).show(childFragmentManager, null)
        }

        binding.sortButton.setOnClickListener {
            val currentSort = model.commentSort.value!!
            showCommentSortPopup(it, currentSort){ sort ->
                model.setCommentSort(sort)
                model.fetchComments(refreshPost = false)
            }
        }

        childFragmentManager.setFragmentResultListener(NEW_REPLY_KEY, viewLifecycleOwner){ _, bundle ->
            val newReply = bundle.get(NEW_REPLY_KEY) as ReplyVM.NewReply
            val comment = newReply.item
            val position = newReply.position
            model.addItems(position, listOf(comment))
            adapter.addItems(position, listOf(comment))
        }

        initBottomBarOnClickListeners(behavior)
    }

    private fun initBottomBarOnClickListeners(behavior: BottomSheetBehavior<CoordinatorLayout>) {

        binding.bottomBarLayout.commentsButton.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.bottomBarLayout.upvoteButton.setOnClickListener {
            if (model.loading.value == true) {
                return@setOnClickListener
            }
            model.post.value?.let {
                it.likes = if (it.likes == true) {
                    null
                } else {
                    true
                }
                val vote = if (it.likes == true) {
                    Vote.UPVOTE
                } else {
                    Vote.UNVOTE
                }
                activityModel.vote(it.name, vote)
                binding.bottomBarLayout.invalidateAll()
            }
        }

        binding.bottomBarLayout.downvoteButton.setOnClickListener {
            if (model.loading.value == true) {
                return@setOnClickListener
            }
            model.post.value?.let {
                it.likes = if (it.likes == false) {
                    null
                } else {
                    false
                }
                val vote = if (it.likes == false) {
                    Vote.DOWNVOTE
                } else {
                    Vote.UNVOTE
                }
                activityModel.vote(it.name, vote)
                binding.bottomBarLayout.invalidateAll()
            }
        }

        binding.bottomBarLayout.saveButton.setOnClickListener {
            if (model.loading.value == true) {
                return@setOnClickListener
            }
            model.post.value?.let {
                it.saved = !it.saved
                activityModel.save(it.name, it.saved)
                binding.bottomBarLayout.invalidateAll()
            }
        }

        binding.bottomBarLayout.moreOptionsButton.setOnClickListener {
            showMoreOptions(it)
        }

    }

    private fun initPost() {
        val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
        val url = requireArguments().get(URL_KEY) as String?
        if (postPage != null) {
            model.setPost(postPage.post)
        } else {
            model.fetchComments(url!!)
            BottomSheetBehavior.from(binding.bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun initPostObservers(){
        model.post.observe(viewLifecycleOwner, Observer { post ->
            if(!model.contentInitialized){
                if(post.crosspostParentList != null){
                    binding.crossPostLayout.cardView.setOnClickListener {
                        viewPagerModel.newPage(PostPage(post.crosspostParentList[0], -1))
                    }
                }
                when{
                    post.isSelf -> markwon.setMarkdown(binding.content.contentText, post.selftext)
                    else -> {
                        when(post.url?.getUrlType()){
                            UrlType.OTHER -> initUrlPreview(post.url)
                            else -> model.fetchMediaItems(post)
                        }
                    }
                }
                model.contentInitialized = true
            }
        })
    }

    private fun initUrlPreview(url: String){
        binding.content.thumbnailWithUrlLayout.root.setOnClickListener {
            handleLink(url)
        }
    }

    private fun initMedia(){
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        model.mediaItems.observe(viewLifecycleOwner, Observer {
            if(it != null){
                val adapter = MediaListFragmentAdapter(this, it)
                binding.content.viewPager.apply {
                    this.adapter = adapter
                    (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
                    if(it.size > 1){
                        registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                            override fun onPageScrollStateChanged(state: Int) {
                                super.onPageScrollStateChanged(state)
                                if(state == ViewPager2.SCROLL_STATE_IDLE){
                                    binding.content.previousButton.visibility = if(currentItem == 0) View.GONE else View.VISIBLE
                                    binding.content.nextButton.visibility = if(currentItem == it.size - 1) View.GONE else View.VISIBLE
                                }
                            }
                        })

                        binding.content.previousButton.visibility = if(currentItem == 0) View.GONE else View.VISIBLE
                        binding.content.nextButton.visibility = if(currentItem == it.size - 1) View.GONE else View.VISIBLE
                    }
                }

                if(it.size > 1){
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    binding.drawerLayout.addDrawerListener(this)

                    val mediaListAdapter =
                        MediaListAdapter { position ->
                            binding.content.viewPager.currentItem = position
                            binding.drawerLayout.closeDrawer(GravityCompat.END)
                        }
                    mediaListAdapter.submitList(it)
                    binding.albumThumbnails.adapter = mediaListAdapter

                    binding.mediaListIcon.setOnClickListener {
                        binding.drawerLayout.openDrawer(GravityCompat.END)
                    }

                    binding.content.previousButton.setOnClickListener {
                        binding.content.viewPager.currentItem -= 1
                    }

                    binding.content.nextButton.setOnClickListener {
                        binding.content.viewPager.currentItem += 1
                    }

                }
            }
        })
    }

    private fun initOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, Observer {
            if(it != null){
                Snackbar.make(binding.bottomBar, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
            if(postPage != null){
                model.fetchComments()
            } else {
                val url = requireArguments().getString(URL_KEY)
                val fullContextLink = requireArguments().getString(FULL_CONTEXT_URL_KEY, null)
                if(model.allCommentsFetched.value == true){
                    model.fetchComments(fullContextLink)
                } else {
                    model.fetchComments(url!!)
                }
            }
        }

        model.loading.observe(viewLifecycleOwner, Observer {
            if(it == false){
                binding.swipeRefresh.isRefreshing = false
            }
        })

        childFragmentManager.setFragmentResultListener(REPORT_KEY, viewLifecycleOwner){ _, bundle ->
            val  position = bundle.getInt(POSITION_KEY, -1)
            if(position == -1 && model.post.value != null){
                model.post.value!!.hidden = true
                binding.postLayout.invalidateAll()
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
        if(!comment.scoreHidden){
            when(vote){
                Vote.UPVOTE -> {
                    when(comment.likes){
                        true -> comment.score--
                        false -> comment.score += 2
                        null -> comment.score++
                    }
                }
                Vote.DOWNVOTE -> {
                    when(comment.likes){
                        true -> comment.score -= 2
                        false -> comment.score++
                        null -> comment.score--
                    }
                }
                Vote.UNVOTE -> {
                    when(comment.likes){
                        true -> comment.score--
                        false -> comment.score++
                    }
                }
            }
        }
        activityModel.vote(comment.name, vote)
    }

    override fun save(comment: Comment) {
        activityModel.save(comment.name, comment.saved)
    }

    override fun share(comment: Comment) {
        ShareCommentOptionsDialogFragment.newInstance(comment).show(parentFragmentManager, null)
    }

    override fun reply(comment: Comment, position: Int) {
        ReplyDialogFragment.newInstance(comment, position + 1).show(childFragmentManager, null)
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

    override fun itemClicked(item: Item, position: Int) {
        when(item) {
            is More -> {
                if (item.isContinueThreadLink) {
                    viewPagerModel.newPage(ContinueThreadPage("${model.post.value!!.permalink}${item.parentId.replace("t1_", "")}", null, true))
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
            UrlType.REDDIT_COMMENTS -> viewPagerModel.newPage(ContinueThreadPage(link, null, true))
            UrlType.OTHER, UrlType.REDDIT_VIDEO -> activityModel.openChromeTab(link)
            UrlType.IMGUR_IMAGE -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.IMGUR_PICTURE)).show(childFragmentManager, null)
            null -> TODO()
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
        val popupWindow = PopupWindow(popupBinding.root, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
        popupBinding.apply {
            sort = commentSort
            best.root.setOnClickListener {
                onSortSelected(CommentSort.BEST)
                popupWindow.dismiss()
            }
            top.root.setOnClickListener {
                onSortSelected(CommentSort.TOP)
                popupWindow.dismiss()
            }
            newSort.root.setOnClickListener {
                onSortSelected(CommentSort.NEW)
                popupWindow.dismiss()
            }
            controversial.root.setOnClickListener {
                onSortSelected(CommentSort.CONTROVERSIAL)
                popupWindow.dismiss()
            }
            old.root.setOnClickListener {
                onSortSelected(CommentSort.OLD)
                popupWindow.dismiss()
            }
            random.root.setOnClickListener {
                onSortSelected(CommentSort.RANDOM)
                popupWindow.dismiss()
            }
            qa.root.setOnClickListener {
                onSortSelected(CommentSort.QA)
                popupWindow.dismiss()
            }

            executePendingBindings()
        }

        popupBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.height = popupBinding.root.measuredHeight
        popupWindow.elevation = 20F
        popupWindow.showAsDropDown(anchor)
    }

    private fun showMoreOptions(anchor: View){
        if(model.post.value == null){
            return
        }

        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupCommentsPageOptionsBinding.inflate(inflater)
        val popupWindow = PopupWindow(popupBinding.root, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
        popupBinding.apply {
            val post = model.post.value!!
            this.post = post
            profileButton.root.setOnClickListener {
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(post.author)))
                popupWindow.dismiss()
            }
            subredditButton.root.setOnClickListener {
                findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(ListingPage(SubredditListing(post.subreddit))))
                popupWindow.dismiss()
            }
            hideButton.root.setOnClickListener {
                post.hidden = !post.hidden
                binding.invalidateAll()
                activityModel.hide(post.name, post.hidden)
                popupWindow.dismiss()
            }
            shareButton.root.setOnClickListener {
                SharePostOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
                popupWindow.dismiss()
            }
            reportButton.root.setOnClickListener {
                ReportDialogFragment.newInstance(post).show(childFragmentManager, null)
                popupWindow.dismiss()
            }

            executePendingBindings()
        }

        popupBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.height = popupBinding.root.measuredHeight
        popupWindow.elevation = 20F
        popupWindow.showAsDropDown(anchor)
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

