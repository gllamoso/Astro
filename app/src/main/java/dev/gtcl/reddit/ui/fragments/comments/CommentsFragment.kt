package dev.gtcl.reddit.ui.fragments.comments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.util.Linkify
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.reddit.*
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.CommentActions
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.LinkHandler
import dev.gtcl.reddit.databinding.FragmentCommentsBinding
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.*
import dev.gtcl.reddit.ui.fragments.media.MediaDialogFragment
import dev.gtcl.reddit.ui.fragments.reply.ReplyDialogFragment
import dev.gtcl.reddit.ui.fragments.reply.ReplyVM
import io.noties.markwon.*
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import io.noties.markwon.utils.NoCopySpannableFactory
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class CommentsFragment : Fragment(), CommentActions, ItemClickListener, LinkHandler {

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

        initPost()
        initTopBar()
        initBottomBarAndCommentsAdapter()
        initOtherObservers()

        binding.executePendingBindings()
        return binding.root
    }

    private fun initOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, Observer {
            if(it != null){
                Snackbar.make(binding.bottomBar, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        model.post.observe(viewLifecycleOwner, Observer { post ->
            if(post.crosspostParentList != null){
                binding.crossPostLayout.cardView.setOnClickListener {
                    viewPagerModel.newPage(PostPage(post.crosspostParentList[0], -1))
                }
            }
        })

        binding.swipeRefresh.setOnRefreshListener {
            val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
            if(postPage != null){
                model.fetchPostAndComments()
            } else {
                val url = requireArguments().getString(URL_KEY)
                val fullContextLink = requireArguments().getString(FULL_CONTEXT_URL_KEY, null)
                if(model.allCommentsFetched.value == true){
                    model.fetchPostAndComments(fullContextLink)
                } else {
                    model.fetchPostAndComments(url!!)
                }
            }
        }

        model.loading.observe(viewLifecycleOwner, Observer {
            if(it == false){
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }

    override fun onPause() {
        super.onPause()
        val position = arguments?.get(POSITION_KEY)
        val post = model.post.value.apply {
            this?.isRead = true
        }
//        parentFragmentManager.setFragmentResult(POST_BUNDLE_KEY, bundleOf(POST_KEY to post, POSITION_KEY to position))
    }

    override fun onStop() {
        super.onStop()
        if (!requireActivity().isChangingConfigurations) {
            model.pausePlayer()
        }
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
                        model.fetchPostAndComments(fullContextLink)
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

        binding.commentsToolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.reply && model.post.value != null) {
                ReplyDialogFragment.newInstance(model.post.value!!, 0).show(childFragmentManager, null)
                true
            } else {
                false
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


    }

    private fun initPost() {
        val postPage = requireArguments().get(POST_PAGE_KEY) as PostPage?
        val url = requireArguments().get(URL_KEY) as String?
        if (postPage != null) {
            if (!model.commentsFetched) {
                model.setPost(postPage.post)
            }
            when (postPage.post.postType) {
                PostType.IMAGE -> initSubsamplingImageView(postPage.post)
                PostType.GIF -> initGifToImageView(postPage.post)
                PostType.VIDEO -> initVideoPlayer(postPage.post)
                PostType.TEXT -> markwon.setMarkdown(binding.content.contentText, postPage.post.selftext)
                PostType.URL -> initUrlPreview(postPage.post)
            }
        } else {
            model.fetchPostAndComments(url!!)
            BottomSheetBehavior.from(binding.bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun initSubsamplingImageView(post: Post) {
        Glide.with(requireContext())
            .asBitmap()
            .load(post.url)
            .addListener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@CommentsFragment.model.loadingFinished()
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@CommentsFragment.model.loadingFinished()
                    return false
                }

            })
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
            .into(SubsamplingScaleImageViewTarget(binding.content.scaleImageView))
    }

    private fun initGifToImageView(post: Post) {
        Glide.with(requireContext())
            .load(post.url)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@CommentsFragment.model.loadingFinished()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@CommentsFragment.model.loadingFinished()
                    return false
                }

            })
            .into(binding.content.imageView)
    }

    private fun initUrlPreview(post: Post){
        binding.content.thumbnailWithUrlLayout.root.setOnClickListener {
            handleLink(post.url ?: "")
        }
    }

    private fun initVideoPlayer(post: Post) {
        model.initializePlayer(post)
        model.player.observe(viewLifecycleOwner, Observer { simpleExoPlayer ->
            if (simpleExoPlayer != null) {
                binding.content.playerView.player = simpleExoPlayer
                model.loadingFinished()
            }
        })
    }

//      _____                                     _                  _   _
//     / ____|                                   | |       /\       | | (_)
//    | |     ___  _ __ ___  _ __ ___   ___ _ __ | |_     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___| (_) | | | | | | | | | | |  __/ | | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    \_____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/

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
        ReplyDialogFragment.newInstance(comment, position + 1).show(childFragmentManager, null)
    }

    override fun viewProfile(comment: Comment) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(comment.author)
            )
        )
    }

    override fun report(comment: Comment) {
        TODO("Not yet implemented")
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
            UrlType.IMGUR_ALBUM -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.IMGUR_ALBUM)).show(childFragmentManager, null)
            UrlType.REDDIT_COMMENTS -> viewPagerModel.newPage(ContinueThreadPage(link, null, true))
            UrlType.OTHER, UrlType.REDDIT_VIDEO -> activityModel.openChromeTab(link)
        }
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

