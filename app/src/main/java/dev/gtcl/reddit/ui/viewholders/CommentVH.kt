package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemCommentBinding
import dev.gtcl.reddit.models.reddit.listing.Comment

class CommentVH private constructor(private val binding: ItemCommentBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(comment: Comment, onCommentClicked: (Int, Comment) -> Unit){
        binding.comment = comment
        itemView.setOnClickListener { onCommentClicked(adapterPosition, comment)}
        if(comment.hiddenPoints > 0){
            itemView.visibility = View.GONE
            itemView.layoutParams = RecyclerView.LayoutParams(0,0)
        }
        else {
            itemView.visibility = View.VISIBLE
            itemView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        binding.commentTextView.visibility = if(comment.isPartiallyCollapsed) View.GONE else View.VISIBLE
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): CommentVH {
            return CommentVH(ItemCommentBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}