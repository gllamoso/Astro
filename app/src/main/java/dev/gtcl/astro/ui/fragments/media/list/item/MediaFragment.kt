package dev.gtcl.astro.ui.fragments.media.list.item

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentMediaBinding
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.media.list.MediaListFragment
import timber.log.Timber
import java.lang.IllegalStateException

class MediaFragment : Fragment() {
    private var binding: FragmentMediaBinding? = null

    private val model: MediaVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(MediaVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private var player: SimpleExoPlayer? = null

    override fun onResume() {
        super.onResume()
        if (player == null && model.mediaURL.value?.mediaType == MediaType.VIDEO) {
            initVideoPlayer(model.mediaURL.value ?: return)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMediaBinding.inflate(inflater)
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.model = model
        binding?.fragmentMediaPlayerController?.hide()

        val mediaURL = requireArguments().get(MEDIA_KEY) as MediaURL
        if (model.mediaURL.value == null && model.isLoading.value != true) {
            val playWhenReady = requireArguments().getBoolean(PLAY_WHEN_READY_KEY)
            model.setMedia(mediaURL, playWhenReady)
        }

        model.mediaURL.observe(viewLifecycleOwner, {
            if (it == null) {
                return@observe
            }
            when (it.mediaType) {
                MediaType.GIF -> initGifToImageView(it)
                MediaType.PICTURE -> initSubsamplingImageView(it)
                MediaType.VIDEO -> initVideoPlayer(it)
                MediaType.VIDEO_PREVIEW -> initVideoPreview(it)
                else -> throw IllegalStateException("Invalid media type: ${mediaURL.mediaType}")
            }
        })

        activityModel.mediaDialogOpened.observe(viewLifecycleOwner, {
            if (it == true && requireParentFragment() !is MediaListFragment) {
                releasePlayer()
                val media = requireArguments().get(MEDIA_KEY) as MediaURL
                model.setMedia(media, false)
            }
        })

        binding?.executePendingBindings()

        return binding?.root
    }

    override fun onPause() {
        super.onPause()
        val isChangingConfigurations = requireActivity().isChangingConfigurations
        if (!isChangingConfigurations) {
            player?.playWhenReady = false
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.fragmentMediaPlayerView?.player = null
        binding?.fragmentMediaPlayerController?.player = null
        binding = null
    }

    private fun releasePlayer() {
        binding?.fragmentMediaPlayerController?.hide()
        player?.let {
            model.playbackPosition = it.currentPosition
            model.currentWindow = it.currentWindowIndex
            model.playWhenReady = it.playWhenReady
        }
        binding?.fragmentMediaPlayerView?.player = null
        binding?.fragmentMediaPlayerController?.player = null
        player?.release()
        player = null
    }

    private fun initSubsamplingImageView(mediaURL: MediaURL) {
        val url = (mediaURL.url.toValidImgUrl() ?: mediaURL.url)
        binding?.fragmentMediaScaleImageView?.let { subsamplingScaleImageView ->
            GlideApp.with(requireContext())
                .asBitmap()
                .load(url)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .apply(
                    RequestOptions()
                        .error(R.drawable.ic_broken_image_24)
                )
                .addListener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        mod: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        model.setLoadingState(false)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        mod: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        model.setLoadingState(false)
                        return false
                    }

                })
                .into(SubsamplingScaleImageViewTarget(subsamplingScaleImageView))
        }

        binding?.fragmentMediaScaleImageView?.setOnClickListener {
            activityModel.toggleUi()
        }
    }

    private fun initGifToImageView(mediaURL: MediaURL) {
        val url = mediaURL.url
        binding?.fragmentMediaImageView?.let { imageView ->
            imageView.clearColorFilter()
            GlideApp.with(requireContext())
                .load(url)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        mod: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        model.setLoadingState(false)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        mod: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        model.setLoadingState(false)
                        return false
                    }

                })
                .into(imageView)
        }

        binding?.fragmentMediaImageView?.setOnClickListener {
            activityModel.toggleUi()
        }
    }

    private fun initVideoPlayer(mediaURL: MediaURL) {
        initExoPlayer(mediaURL)

        activityModel.showMediaControls.observe(viewLifecycleOwner, {
            if (it) {
                binding?.fragmentMediaPlayerController?.show()
            } else {
                binding?.fragmentMediaPlayerController?.hide()
            }
        })

        binding?.root?.setOnClickListener {
            activityModel.toggleUi()
        }
    }

    private fun initExoPlayer(mediaURL: MediaURL) {
        val videoUri = Uri.parse(mediaURL.url)
        var mediaSource = buildMediaSource(requireContext(), videoUri)
        player =
            ExoPlayerFactory.newSimpleInstance(
                requireContext()
            )
        binding?.apply {
            fragmentMediaPlayerView.player = player
            fragmentMediaPlayerController.player = player
        }
        player?.apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = model.playWhenReady
            seekTo(model.currentWindow, model.playbackPosition)
            prepare(mediaSource, false, false)
            addListener(object : Player.EventListener {
                override fun onPlayerError(error: ExoPlaybackException?) {
                    Timber.tag("Media").d("Exception $error")
                    if (videoUri.path != mediaURL.backupUrl && mediaURL.backupUrl != null) {
                        mediaSource = buildMediaSource(
                            requireContext(),
                            Uri.parse(mediaURL.backupUrl)
                        )
                        prepare(mediaSource, false, false)
                    }
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    super.onPlayerStateChanged(playWhenReady, playbackState)
                    if (playbackState == Player.STATE_READY) {
                        model.setLoadingState(false)
                    }
                }
            })
        }
    }

    private fun initVideoPreview(mediaURL: MediaURL) {
        val url = (mediaURL.thumbnail ?: mediaURL.url).replaceFirst("http://", "https://")
        binding?.fragmentMediaImageView?.let { imageView ->
            imageView.setColorFilter(Color.argb(155, 40, 40, 40), PorterDuff.Mode.SRC_ATOP)
            val glideBuilder = GlideApp.with(requireContext())
                .load(url)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        mod: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        model.setLoadingState(false)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        mod: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        model.setLoadingState(false)
                        return false
                    }
                })

            if (mediaURL.thumbnail == null) {
                val requestOptions = RequestOptions().frame(1000L)
                glideBuilder.apply(requestOptions)
            }

            glideBuilder.into(imageView)
        }

        binding?.fragmentMediaPlayPreview?.setOnClickListener {
            val mediaURLFromArguments = requireArguments().get(MEDIA_KEY) as MediaURL
            model.setMedia(mediaURLFromArguments, true)
        }
    }

    companion object {
        fun newInstance(mediaURL: MediaURL, playWhenReady: Boolean): MediaFragment {
            val fragment = MediaFragment()
            val args = bundleOf(MEDIA_KEY to mediaURL, PLAY_WHEN_READY_KEY to playWhenReady)
            fragment.arguments = args
            return fragment
        }
    }
}