package dev.gtcl.reddit.ui.fragments.media.list.item

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentMediaBinding
import dev.gtcl.reddit.models.reddit.MediaURL
import java.lang.IllegalStateException

class MediaItemFragment : Fragment(){
    private lateinit var binding: FragmentMediaBinding

    private val model: MediaItemVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MediaItemVM::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMediaBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model

        val mediaURL = requireArguments().get(MEDIA_KEY) as MediaURL
        if(!model.initialized){
            model.setMedia(mediaURL)
        }
        when(mediaURL.mediaType){
            MediaType.GIF -> initGifToImageView()
            MediaType.PICTURE -> initSubsamplingImageView()
            MediaType.VIDEO, MediaType.GFYCAT -> initVideoPlayer()
            else -> throw IllegalStateException("Invalid media type: ${mediaURL.mediaType}")
        }

        binding.executePendingBindings()

        return binding.root
    }

    override fun onPause() {
        super.onPause()
        if(!requireActivity().isChangingConfigurations){
            model.pausePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if(requireActivity().isChangingConfigurations){
            model.resumePlayer()
        }
    }

    private fun initSubsamplingImageView(){
        model.mediaURL.observe(viewLifecycleOwner, Observer {
            Glide.with(requireContext())
                .asBitmap()
                .load(it.url)
                .addListener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                })
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                .into(SubsamplingScaleImageViewTarget(binding.scaleImageView))
        })

        binding.scaleImageView.setOnClickListener {
//            model.toggleUiVisibility()
        }
    }

    private fun initGifToImageView(){

        model.mediaURL.observe(viewLifecycleOwner, Observer {
            Glide.with(requireContext())
                .load(it.url)
                .addListener(object: RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                })
                .into(binding.imageView)
        })

        binding.imageView.setOnClickListener {
//            model.toggleUiVisibility()
        }
    }

    private fun initVideoPlayer(){
        model.player.observe(viewLifecycleOwner, Observer {simpleExoPlayer ->
            if(simpleExoPlayer != null) {
                binding.playerView.player = simpleExoPlayer
                binding.playerController.player = simpleExoPlayer
//                model.loadingFinished()
//                model.playerControllerView.observe(viewLifecycleOwner, Observer { weakReference ->
//                    if(weakReference != null){
//                        weakReference.get()?.player = simpleExoPlayer
//                        model.playerControlViewObserved()
//                    }
//                })
            }
        })

        binding.root.setOnClickListener {
//            model.toggleUiVisibility()
        }
    }


    companion object{
        fun newInstance(mediaURL: MediaURL): MediaItemFragment {
            val fragment =
                MediaItemFragment()
            val args = bundleOf(MEDIA_KEY to mediaURL)
            fragment.arguments = args
            return fragment
        }
    }
}