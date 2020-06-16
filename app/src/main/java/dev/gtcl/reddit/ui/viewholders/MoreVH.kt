package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemMoreCommentBinding
import dev.gtcl.reddit.models.reddit.listing.More

class MoreVH private constructor(private val binding: ItemMoreCommentBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(item: More, onMoreClicked: (Int) -> Unit){
        binding.more = item
        if(item.hiddenPoints > 0){
            itemView.visibility = View.GONE
            itemView.layoutParams = RecyclerView.LayoutParams(0,0)
        }
        else {
            itemView.visibility = View.VISIBLE
            itemView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        binding.commentTextView.setOnClickListener { onMoreClicked(adapterPosition) }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): MoreVH {
            return MoreVH(ItemMoreCommentBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}