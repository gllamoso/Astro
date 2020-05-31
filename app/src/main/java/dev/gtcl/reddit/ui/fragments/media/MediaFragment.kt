package dev.gtcl.reddit.ui.fragments.media

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.ui.PlayerControlView
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentMediaViewerBinding
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.models.reddit.UrlType
import java.lang.ref.WeakReference
import kotlin.IllegalArgumentException

class MediaFragment: Fragment(){
    private lateinit var binding: FragmentMediaViewerBinding

    val model: MediaVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(MediaVM::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMediaViewerBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model

        model.loadingStarted()

        model.urlType.observe(viewLifecycleOwner, Observer {
            when(it) {
                UrlType.IMAGE -> setSubsamplingImageView()
                UrlType.GIF -> setGifToImageView()
                UrlType.GFYCAT, UrlType.M3U8, UrlType.GIFV -> setVideoPlayer()
                else -> throw IllegalArgumentException("Invalid URL Type: $it")
            }
        })

        binding.executePendingBindings()

        return binding.root
    }

    override fun onStop() {
        super.onStop()
        if(!requireActivity().isChangingConfigurations){
            model.pausePlayer()
        }
    }

    private fun setSubsamplingImageView(){

        model.url.observe(viewLifecycleOwner, Observer {
            Glide.with(requireContext())
                .asBitmap()
                .load(it)
                .addListener(object : RequestListener<Bitmap>{
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        this@MediaFragment.model.loadingFinished()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        this@MediaFragment.model.loadingFinished()
                        return false
                    }

                })
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                .into(SubsamplingScaleImageViewTarget(binding.scaleImageView))
        })

        binding.scaleImageView.setOnClickListener {
            model.toggleUiVisibility()
        }
    }

    private fun setGifToImageView(){

        model.url.observe(viewLifecycleOwner, Observer {
            Glide.with(requireContext())
                .load(model.url)
                .addListener(object: RequestListener<Drawable>{
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        this@MediaFragment.model.loadingFinished()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        this@MediaFragment.model.loadingFinished()
                        return false
                    }

                })
                .into(binding.imageView)
        })

        binding.imageView.setOnClickListener {
            model.toggleUiVisibility()
        }
    }

    private fun setVideoPlayer(){
        model.initializePlayer()

        model.player.observe(viewLifecycleOwner, Observer {simpleExoPlayer ->
            if(simpleExoPlayer != null) {
                binding.playerView.player = simpleExoPlayer
                model.loadingFinished()
                model.playerControllerView.observe(viewLifecycleOwner, Observer { weakReference ->
                    if(weakReference != null){
                        weakReference.get()?.player = simpleExoPlayer
                        model.playerControlViewObserved()
                    }
                })
            }
        })

        binding.root.setOnClickListener {
            model.toggleUiVisibility()
        }
    }


    companion object{
        fun newInstance(): MediaFragment{
            return MediaFragment()
        }
    }

}