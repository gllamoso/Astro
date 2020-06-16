package dev.gtcl.reddit.ui.fragments.comments

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentCommentsBinding
import dev.gtcl.reddit.actions.ViewPagerActions
import dev.gtcl.reddit.models.reddit.listing.More
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.models.reddit.listing.PostType

class CommentsFragment : Fragment() {

    private val model: CommentsVM by lazy {
        val viewModelFactory = ViewModelFactory(requireContext().applicationContext as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(CommentsVM::class.java)
    }

    private lateinit var binding: FragmentCommentsBinding

    private var viewPagerActions: ViewPagerActions? = null

    fun setActions(viewPagerActions: ViewPagerActions){
        this.viewPagerActions = viewPagerActions
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCommentsBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model

        initPost()
        initCommentsAdapter()

        binding.executePendingBindings()
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        if(!requireActivity().isChangingConfigurations){
            model.pausePlayer()
        }
    }

    private fun initCommentsAdapter(){
        val adapter = CommentsAdapter(object : CommentsAdapter.CommentItemClickListener{
            override fun onMoreCommentsClicked(position: Int, more: More) {
                model.fetchMoreComments(position, more)
            }

            override fun onContinueThreadClicked(more: More) {
                val url = "${model.post.value?.permalink}${more.parentId.replace("t1_","")}"
                viewPagerActions?.navigateToNewPage(model.post.value!!)
            }
        })
        binding.bottomSheet.commentList.adapter = adapter
        model.comments.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.submitList(it)
            }
        })

        model.moreComments.observe(viewLifecycleOwner, Observer {
            if(it != null) {
                adapter.addItems(it.position, it.comments)
                model.clearMoreComments()
            }
        })

        binding.toolbar.setNavigationOnClickListener {
            viewPagerActions?.navigatePreviousPage()
        }

        val behavior = BottomSheetBehavior.from(binding.bottomSheet.bottomSheet)
        behavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){
            override fun onSlide(p0: View, p1: Float) {
                viewPagerActions?.enablePagerSwiping(false)
            }

            override fun onStateChanged(p0: View, newState: Int) {
                when(newState){
                    BottomSheetBehavior.STATE_HIDDEN, BottomSheetBehavior.STATE_COLLAPSED ->  viewPagerActions?.enablePagerSwiping(true)
                    else ->  viewPagerActions?.enablePagerSwiping(false)
                }
            }
        })
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        binding.bottomBar.commentsButton.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun initPost(){
        val post = requireArguments().get(POST_KEY) as Post
        if(!model.commentsFetched){
            model.setPost(post)
            when (post.postType) {
                PostType.IMAGE -> initSubsamplingImageView(post)
                PostType.GIF -> initGifToImageView(post)
                PostType.VIDEO -> initVideoPlayer(post)
            }
        }
    }

    private fun initSubsamplingImageView(post: Post){
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

        binding.content.scaleImageView.setOnClickListener {
            model.toggleShowMediaButtons()
        }
    }

    private fun initGifToImageView(post: Post){
        Glide.with(requireContext())
            .load(post.url)
            .addListener(object: RequestListener<Drawable>{
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

        binding.content.imageView.setOnClickListener {
            model.toggleShowMediaButtons()
        }
    }

    private fun initVideoPlayer(post: Post){
        model.initializePlayer(post)
        model.player.observe(viewLifecycleOwner, Observer {simpleExoPlayer ->
            if(simpleExoPlayer != null) {
                binding.content.playerView.player = simpleExoPlayer
                binding.content.mediaButtons.playerController.player = simpleExoPlayer
                model.loadingFinished()
            }
        })

        binding.content.root.setOnClickListener {
            model.toggleShowMediaButtons()
        }
    }

    companion object{
        fun newInstance(post: Post): CommentsFragment{
            val fragment = CommentsFragment()
            val args = bundleOf(POST_KEY to post)
            fragment.arguments = args
            return fragment
        }
    }
}
