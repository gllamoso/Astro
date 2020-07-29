package dev.gtcl.reddit.ui.fragments.comments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import dev.gtcl.reddit.*
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.CommentActions
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.LinkHandler
import dev.gtcl.reddit.databinding.FragmentCommentsBinding
import dev.gtcl.reddit.actions.ViewPagerActions
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.*
import dev.gtcl.reddit.ui.fragments.media.MediaDialogFragment
import dev.gtcl.reddit.ui.fragments.reply.ReplyDialogFragment
import dev.gtcl.reddit.ui.fragments.reply.ReplyVM
import io.noties.markwon.*

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

    private var viewPagerActions: ViewPagerActions? = null

    private val markwon: Markwon by lazy {
        Markwon.builder(requireContext())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver(object : LinkResolverDef() {
                        override fun resolve(view: View, link: String) {
                            handleLink(link)
                        }
                    })
                }
            })
            .build()
    }

    fun setActions(viewPagerActions: ViewPagerActions) {
        this.viewPagerActions = viewPagerActions
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

        binding.executePendingBindings()
        return binding.root
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
            viewPagerActions?.navigatePreviousPage()
        }
    }

    private fun initBottomBarAndCommentsAdapter() {
        adapter = CommentsAdapter(markwon,this, this)
        binding.bottomSheet.commentList.adapter = adapter
        model.comments.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                adapter.submitList(it)
            }
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

        val behavior = BottomSheetBehavior.from(binding.bottomSheet.bottomSheet)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(p0: View, p1: Float) {
                viewPagerActions?.enablePagerSwiping(false)
            }

            override fun onStateChanged(p0: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN, BottomSheetBehavior.STATE_COLLAPSED -> viewPagerActions?.enablePagerSwiping(true)
                    else -> viewPagerActions?.enablePagerSwiping(false)
                }
            }
        })

        binding.bottomSheet.toolbar.setOnMenuItemClickListener {
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

        binding.bottomBar.commentsButton.setOnClickListener {
            if (model.loading.value == true) {
                return@setOnClickListener
            }
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.bottomBar.upvoteButton.setOnClickListener {
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
                binding.bottomBar.invalidateAll()
            }
        }

        binding.bottomBar.downvoteButton.setOnClickListener {
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
                binding.bottomBar.invalidateAll()
            }
        }

        binding.bottomBar.saveButton.setOnClickListener {
            if (model.loading.value == true) {
                return@setOnClickListener
            }
            model.post.value?.let {
                it.saved = !it.saved
                activityModel.save(it.name, it.saved)
                binding.bottomBar.invalidateAll()
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
            model.fetchPostAndComments(url!!.replace("http[s]?://www\\.reddit\\.com/".toRegex(), ""))
            BottomSheetBehavior.from(binding.bottomSheet.bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
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
        if (item is More) {
            if (item.isContinueThreadLink) {
                viewPagerModel.newPage(ContinueThreadPage("${model.post.value!!.permalink}${item.parentId.replace("t1_", "")}"))
            } else {
                model.fetchMoreComments(position)
            }
        }
    }

    companion object {
        fun newInstance(postPage: PostPage): CommentsFragment {
            val fragment = CommentsFragment()
            val args = bundleOf(POST_PAGE_KEY to postPage)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(url: String): CommentsFragment {
            val fragment = CommentsFragment()
            val args = bundleOf(URL_KEY to url)
            fragment.arguments = args
            return fragment
        }
    }

    override fun handleLink(link: String) {
        when(link.getUrlType()){
            UrlType.IMAGE -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.PICTURE)).show(childFragmentManager, null)
            UrlType.GIF -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.GIF)).show(childFragmentManager, null)
            UrlType.GIFV, UrlType.HLS, UrlType.STANDARD_VIDEO -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.VIDEO)).show(childFragmentManager, null)
            UrlType.GFYCAT -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.GFYCAT)).show(childFragmentManager, null)
            UrlType.IMGUR_ALBUM -> MediaDialogFragment.newInstance(MediaURL(link, MediaType.IMGUR_ALBUM)).show(childFragmentManager, null)
            UrlType.REDDIT_COMMENTS -> viewPagerModel.newPage(ContinueThreadPage(link))
            UrlType.OTHER, UrlType.REDDIT_VIDEO -> activityModel.openChromeTab(link)
        }
    }
}

