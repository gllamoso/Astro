package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.CommentActions
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.databinding.ItemMoreCommentBinding
import dev.gtcl.reddit.models.reddit.listing.More

class MoreVH private constructor(private val binding: ItemMoreCommentBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: More, itemClickListener: ItemClickListener){
        binding.more = item
        binding.commentTextView.setOnClickListener {
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