package dev.gtcl.astro.ui.fragments.media.list.item

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.FragmentMediaBinding
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.ui.activities.MainActivityVM
import timber.log.Timber
import java.lang.IllegalStateException

class MediaFragment : Fragment() {
    private var binding: FragmentMediaBinding? = null

    private val model: MediaVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(MediaVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    override fun onPause() {
        super.onPause()
        val isChangingConfigurations = requireActivity().isChangingConfigurations
        if (!isChangingConfigurations) {
            model.pausePlayer()
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

        val mediaURL = requireArguments().get(MEDIA_KEY) as MediaURL
        if (!model.initialized) {
            model.setMedia(mediaURL)
        }
        when (mediaURL.mediaType) {
            MediaType.GIF -> initGifToImageView()
            MediaType.PICTURE -> initSubsamplingImageView()
            MediaType.VIDEO, MediaType.GFYCAT, MediaType.REDGIFS -> initVideoPlayer()
            else -> throw IllegalStateException("Invalid media type: ${mediaURL.mediaType}")
        }

        binding?.executePendingBindings()

        return binding!!.root
    }

    private fun initSubsamplingImageView() {
        binding?.fragmentMediaPlayerController?.hide()
        model.mediaURL.observe(viewLifecycleOwner, {
            val url =
                (IMAGE_REGEX.find(it.url)?.value ?: it.url).replaceFirst("http://", "https://")
            binding?.fragmentMediaScaleImageView?.let { subsamplingScaleImageView ->
                Glide.with(requireContext())
                    .asBitmap()
                    .load(url)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
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
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                    .into(SubsamplingScaleImageViewTarget(subsamplingScaleImageView))
            }
        })

        binding?.fragmentMediaScaleImageView?.setOnClickListener {
            activityModel.toggleUi()
        }
    }

    private fun initGifToImageView() {

        binding?.fragmentMediaPlayerController?.hide()
        model.mediaURL.observe(viewLifecycleOwner, {
            val url = it.url.replaceFirst("http://", "https://")
            binding?.fragmentMediaImageView?.let { imageView ->
                Glide.with(requireContext())
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
                    .into(imageView)
            }
        })

        binding?.fragmentMediaImageView?.setOnClickListener {
            activityModel.toggleUi()
        }
    }

    private fun initVideoPlayer() {
        model.player.observe(viewLifecycleOwner, { simpleExoPlayer ->
            if (simpleExoPlayer != null) {
                binding?.fragmentMediaPlayerView?.player = simpleExoPlayer
                binding?.fragmentMediaPlayerController?.player = simpleExoPlayer
                model.setLoadingState(false)
            }
        })

        activityModel.showUi.observe(viewLifecycleOwner, {
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

    override fun onDestroyView() {
        super.onDestroyView()
        val isChangingConfigurations = requireActivity().isChangingConfigurations
        if (!isChangingConfigurations) {
            model.releasePlayer()
            model.initialized = false
        }
        Glide.get(requireContext()).clearMemory()
        binding = null
    }

    companion object {
        fun newInstance(mediaURL: MediaURL): MediaFragment {
            val fragment = MediaFragment()
            val args = bundleOf(MEDIA_KEY to mediaURL)
            fragment.arguments = args
            return fragment
        }
    }
}