package dev.gtcl.reddit.ui.fragments.dialog.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import dev.gtcl.reddit.databinding.LayoutPopupVideoOptionsBinding
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.models.reddit.UrlType
import kotlin.IllegalArgumentException

class MediaFragment: Fragment(){
    private lateinit var binding: FragmentMediaViewerBinding
    lateinit var controllerView: PlayerControlView // TODO: use weak reference
    lateinit var showUiCallback: () -> Unit
    lateinit var postUrlCallback: (Post) -> Unit

    val model: MediaViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MediaViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMediaViewerBinding.inflate(inflater)
        binding.model = model
        if(!model.initialized){
            val url = requireArguments().get(URL_KEY) as String
            val urlType = requireArguments().get(URL_TYPE_KEY) as UrlType
            val post = requireArguments().get(POST_KEY) as Post?
            model.url = url
            model.setUrlType(urlType)
            model.post = post
            model.initialized = true
        }

        model.setLoading(model.player.value == null)
        when(model.urlType.value){
            UrlType.IMAGE -> setSubsamplingImageView()
            UrlType.GIF -> setGifToImageView()
            UrlType.GFYCAT, UrlType.M3U8, UrlType.GIFV -> setVideoPlayer()
            else -> throw IllegalArgumentException("Invalid URL Type: ${model.urlType.value}")
        }
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
        Glide.with(requireContext())
            .asBitmap()
            .load(model.url)
            .addListener(object : RequestListener<Bitmap>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@MediaFragment.model.setLoading(false)
                    binding.invalidateAll()
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@MediaFragment.model.setLoading(false)
                    binding.invalidateAll()
                    return false
                }

            })
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
            .into(SubsamplingScaleImageViewTarget(binding.scaleImageView))

        binding.scaleImageView.setOnClickListener {
            showUiCallback()
        }
    }

    private fun setGifToImageView(){
        Glide.with(requireContext())
            .load(model.url)
            .addListener(object: RequestListener<Drawable>{
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@MediaFragment.model.setLoading(false)
                    binding.invalidateAll()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    this@MediaFragment.model.setLoading(false)
                    binding.invalidateAll()
                    return false
                }

            })
            .into(binding.imageView)

        binding.imageView.setOnClickListener {
            showUiCallback()
        }
    }

    private fun setVideoPlayer(){
        model.initializePlayer()

        model.player.observe(viewLifecycleOwner, Observer {
            if(it != null) {
                binding.playerView.player = it
                if(::controllerView.isInitialized){
                    controllerView.player = it
                }
                model.setLoading(false)
                binding.invalidateAll()
            }
        })

        binding.root.setOnClickListener {
            showUiCallback()
        }
    }


    companion object{
        fun newInstance(url: String, urlType: UrlType, post: Post? = null): MediaFragment{
            val fragment = MediaFragment()
            val args = bundleOf(URL_KEY to url, URL_TYPE_KEY to urlType, POST_KEY to post)
            fragment.arguments = args
            return fragment
        }
    }

}