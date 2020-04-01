package dev.gtcl.reddit.ui.fragments.home

import android.content.DialogInterface
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.gtcl.reddit.R
import dev.gtcl.reddit.buildMediaSource
import dev.gtcl.reddit.databinding.FragmentDialogImageVideoViewerBinding
import dev.gtcl.reddit.listings.Post

class ImageVideoViewerDialogFragment : BottomSheetDialogFragment(){

    private lateinit var binding: FragmentDialogImageVideoViewerBinding
    private lateinit var post: Post

    private var mPlayer: SimpleExoPlayer? = null
    private var mPlayWhenReady = true
    private var mCurrentWindow = 0
    private var mPlaybackPosition = 0.toLong()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDialogImageVideoViewerBinding.inflate(inflater)
        when{
            post.preview?.redditVideo != null -> setPlayerView(post.preview!!.redditVideo!!.hlsUrl)
            post.secureMedia?.redditVideo != null -> setPlayerView(post.secureMedia!!.redditVideo!!.hlsUrl)
            post.media?.redditVideo != null -> setPlayerView(post.media!!.redditVideo!!.hlsUrl)
            post.isPicture() -> setImageView(post.url!!)
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
        }

        val view = requireView()
        view.post {
            val parent = view.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior
            val bottomSheetBehavior = behavior as BottomSheetBehavior<*>
            bottomSheetBehavior.peekHeight = view.measuredHeight
            (view.parent as View).setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun setPost(post: Post){
        this.post = post
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        releasePlayer()
    }

    private fun setPlayerView(videoUrl: String){
        initializePlayer(videoUrl)
        binding.playerView.visibility = View.VISIBLE
        binding.imageView.visibility = View.GONE
        binding.contentBackground.setBackgroundColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
    }

    private fun setImageView(url: String){
        releasePlayer()
        binding.playerView.visibility = View.GONE
        binding.imageView.visibility = View.VISIBLE
        binding.contentBackground.setBackgroundColor(ContextCompat.getColor(binding.root.context, android.R.color.black))

        val imgUri = url.toUri().buildUpon().scheme("https").build()
        Glide.with(requireContext())
            .load(imgUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.anim_loading)
                    .error(R.drawable.ic_broken_image))
            .into(binding.imageView)
    }

    private fun releasePlayer() {
        if(mPlayer != null){
            mPlayWhenReady = mPlayer!!.playWhenReady
            mPlaybackPosition = mPlayer!!.currentPosition
            mCurrentWindow = mPlayer!!.currentWindowIndex
            mPlayer!!.release()
            mPlayer = null
        }
    }

    private fun initializePlayer(url: String){
        if(mPlayer == null){
            val trackSelector = DefaultTrackSelector()
            trackSelector.setParameters(trackSelector.buildUponParameters().setMaxVideoSizeSd())
            mPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
            mPlayer!!.repeatMode = Player.REPEAT_MODE_ONE
        }

        binding.playerView.player = mPlayer
        val uri = Uri.parse(url)
        val mediaSource = buildMediaSource(requireContext(), uri)
        mPlayer!!.apply {
            playWhenReady = mPlayWhenReady
            seekTo(mCurrentWindow, mPlaybackPosition)
            prepare(mediaSource, false, false)
        }
    }
}