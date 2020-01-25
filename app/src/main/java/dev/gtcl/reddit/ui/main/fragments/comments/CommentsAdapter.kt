package dev.gtcl.reddit.ui.main.fragments.comments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.comments.Comment
import dev.gtcl.reddit.comments.CommentItem
import dev.gtcl.reddit.comments.More
import dev.gtcl.reddit.databinding.ItemCommentBinding
import dev.gtcl.reddit.databinding.ItemMoreCommentBinding

class CommentsAdapter(private val commentItemClickListener: CommentItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var mCommentItems = mutableListOf<CommentItem>()

    fun submitList(items: List<CommentItem>){
        mCommentItems = items.toMutableList()
        notifyDataSetChanged()
    }

    fun addItems(position: Int, items: List<CommentItem>){
        mCommentItems.removeAt(position)
        notifyItemRemoved(position)
        mCommentItems.addAll(position, items)
        notifyItemRangeInserted(position, items.size)
        notifyItemRangeChanged(position + items.size, mCommentItems.size - (position + items.size + 1))

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_more_comment -> MoreViewHolder.create(parent)
            R.layout.item_comment -> CommentViewHolder.create(parent)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val commentItem = mCommentItems[position]
        if(commentItem is Comment)
            (holder as CommentViewHolder).bind(commentItem)
        if(commentItem is More)
            (holder as MoreViewHolder).bind(commentItem) {
                if(commentItem.isContinueThreadLink())
                    commentItemClickListener.onContinueThreadClicked(commentItem)
                else
                    commentItemClickListener.onMoreCommentsClicked(position, commentItem)
            }
    }

    override fun getItemViewType(position: Int): Int {
        return if(mCommentItems[position] is More)
                R.layout.item_more_comment
            else
                R.layout.item_comment
    }

    override fun getItemCount(): Int = mCommentItems.size

    class CommentViewHolder private constructor(private var binding: ItemCommentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment){
            binding.comment = comment
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup): CommentViewHolder {
                return CommentViewHolder(ItemCommentBinding.inflate(LayoutInflater.from(parent.context)))
            }
        }
    }

    class MoreViewHolder private constructor(private var binding: ItemMoreCommentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(more: More, onMoreClicked: () -> Unit){
            binding.more = more
            binding.root.setOnClickListener { onMoreClicked() }
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup): MoreViewHolder {
                return MoreViewHolder(ItemMoreCommentBinding.inflate(LayoutInflater.from(parent.context)))
            }
        }
    }

    interface CommentItemClickListener{
        fun onMoreCommentsClicked(position: Int, more: More)
        fun onContinueThreadClicked(more: More)
    }

}
