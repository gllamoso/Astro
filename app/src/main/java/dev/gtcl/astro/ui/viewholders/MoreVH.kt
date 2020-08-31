package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.ItemMoreCommentBinding
import dev.gtcl.astro.models.reddit.listing.More

class MoreVH private constructor(private val binding: ItemMoreCommentBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: More, itemClickListener: ItemClickListener){
        binding.more = item
        binding.itemMoreCommentText.setOnClickListener {
            itemClickListener.itemClicked(item, adapterPosition)
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): MoreVH {
            return MoreVH(ItemMoreCommentBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}