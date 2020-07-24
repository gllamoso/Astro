package dev.gtcl.reddit.ui.fragments.media.test.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.gtcl.reddit.models.reddit.MediaURL
import dev.gtcl.reddit.ui.viewholders.MediaVH

class MediaListRecyclerViewAdapter(private val itemClickListener: (Int) -> Unit) : ListAdapter<MediaURL, MediaVH>(DiffCallback){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MediaVH.create(parent)

    override fun onBindViewHolder(holder: MediaVH, position: Int) {
        holder.bind(getItem(position), itemClickListener)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MediaURL>(){
        override fun areItemsTheSame(oldItem: MediaURL, newItem: MediaURL) = oldItem.url == newItem.url

        override fun areContentsTheSame(oldItem: MediaURL, newItem: MediaURL) = oldItem == newItem
    }
}