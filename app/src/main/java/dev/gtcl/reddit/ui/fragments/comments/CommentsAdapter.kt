package dev.gtcl.reddit.ui.fragments.comments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.ItemCommentBinding
import dev.gtcl.reddit.databinding.ItemMoreCommentBinding
import dev.gtcl.reddit.network.Comment
import dev.gtcl.reddit.network.ListingItem
import dev.gtcl.reddit.network.More

class CommentsAdapter(private val commentItemClickListener: CommentItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var mCommentItems = mutableListOf<ListingItem>()

    private val collapseComments: (Int) -> Unit = { // TODO: Interface? Add method to CommentItemClickListener?
        val collapse = !(mCommentItems[it] as Comment).isPartiallyCollapsed
        (mCommentItems[it] as Comment).isPartiallyCollapsed = collapse
        val pDepth = mCommentItems[it].depth
        var cIndex = it
        while(++cIndex < itemCount - 1 && mCommentItems[cIndex].depth > pDepth){
            mCommentItems[cIndex].hiddenPoints += if(collapse) 1 else -1
        }
        notifyItemRangeChanged(it, cIndex - it)
    }


    fun submitList(items: List<ListingItem>){
        mCommentItems = items.toMutableList()
        notifyDataSetChanged()
    }

    fun addItems(position: Int, items: List<ListingItem>){
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
        when(val commentItem = mCommentItems[position]){
            is Comment -> (holder as CommentViewHolder).bind(commentItem) { pos, _ -> collapseComments(pos)}
            is More -> (holder as MoreViewHolder).bind(commentItem) {
                if(commentItem.isContinueThreadLink())
                    commentItemClickListener.onContinueThreadClicked(commentItem)
                else
                    commentItemClickListener.onMoreCommentsClicked(position, commentItem)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(mCommentItems[position] is More)
                R.layout.item_more_comment
            else
                R.layout.item_comment
    }

    override fun getItemCount(): Int = mCommentItems.size

    class CommentViewHolder private constructor(private val binding: ItemCommentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment, onCommentClicked: (Int, Comment) -> Unit){
            binding.comment = comment
            itemView.setOnClickListener { onCommentClicked(adapterPosition, comment)}
            if(comment.hiddenPoints > 0){
                itemView.visibility = View.GONE
                itemView.layoutParams = RecyclerView.LayoutParams(0,0)
            }
            else {
                itemView.visibility = View.VISIBLE
                itemView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            binding.commentTextView.visibility = if(comment.isPartiallyCollapsed) View.GONE else View.VISIBLE
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup): CommentViewHolder {
                return CommentViewHolder(ItemCommentBinding.inflate(LayoutInflater.from(parent.context)))
            }
        }
    }

    class MoreViewHolder private constructor(private val binding: ItemMoreCommentBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: More, onMoreClicked: () -> Unit){
            binding.more = item
            if(item.hiddenPoints > 0){
                itemView.visibility = View.GONE
                itemView.layoutParams = RecyclerView.LayoutParams(0,0)
            }
            else {
                itemView.visibility = View.VISIBLE
                itemView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            binding.commentTextView.setOnClickListener { onMoreClicked() }
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
