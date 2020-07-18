package dev.gtcl.reddit.ui.fragments.create_post.flair

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.gtcl.reddit.models.reddit.listing.Flair
import dev.gtcl.reddit.ui.viewholders.FlairVH


class FlairListAdapter(private val flairSelectionListener: FlairSelectionListener): ListAdapter<Flair, FlairVH>(DiffCallback){


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlairVH = FlairVH.create(parent)

    override fun onBindViewHolder(holder: FlairVH, position: Int) {
        holder.bind(getItem(position), flairSelectionListener)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Flair>(){
        override fun areItemsTheSame(oldItem: Flair, newItem: Flair) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Flair, newItem: Flair) = oldItem == newItem
    }

    interface FlairSelectionListener{
        fun onSelect(flair: Flair?)
        fun onEdit(flair: Flair)
    }
}