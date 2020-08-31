package dev.gtcl.astro.ui.fragments.media.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.ui.viewholders.MediaVH

class MediaListAdapter(private val itemClickListener: (Int) -> Unit) : ListAdapter<MediaURL, MediaVH>(
    DiffCallback
){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MediaVH.create(parent)

    override fun onBindViewHolder(holder: MediaVH, position: Int) {
        holder.bind(getItem(position), itemClickListener)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MediaURL>(){
        override fun areItemsTheSame(oldItem: MediaURL, newItem: MediaURL) = oldItem.url == newItem.url

        override fun areContentsTheSame(oldItem: MediaURL, newItem: MediaURL) = oldItem == newItem
    }
}