package dev.gtcl.astro.ui.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import dev.gtcl.astro.MediaType
import dev.gtcl.astro.databinding.ItemMediaSelectableBinding
import dev.gtcl.astro.models.reddit.MediaURL


class MediaVH private constructor(private val binding: ItemMediaSelectableBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(mediaUrl: MediaURL, itemClickListener: (Int) -> Unit) {
        if (mediaUrl.mediaType == MediaType.VIDEO) {
            setVideoPreviewImage(binding.root.context, mediaUrl.url)
        } else {
            setImage(binding.root.context, mediaUrl.url)
        }
        binding.itemMediaSelectableImage.setOnClickListener {
            itemClickListener(adapterPosition)
        }
    }

    private fun setImage(context: Context, url: String) {
        val requestOptions = RequestOptions().fitCenter().override(128, 128)

        Glide.with(context).load(url)
            .apply(requestOptions)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.itemMediaSelectableImage)
    }

    private fun setVideoPreviewImage(context: Context, url: String) {
        val thumb = layoutPosition * 1000.toLong()
        val requestOptions = RequestOptions().fitCenter().override(128, 128)
        val options = RequestOptions().frame(thumb)
        Glide.with(context).load(url).apply(options)
            .apply(requestOptions)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.itemMediaSelectableImage)
    }

    companion object {
        fun create(parent: ViewGroup): MediaVH {
            return MediaVH(ItemMediaSelectableBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}