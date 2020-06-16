package dev.gtcl.reddit.ui.fragments.comments

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.More
import dev.gtcl.reddit.ui.viewholders.CommentVH
import dev.gtcl.reddit.ui.viewholders.MoreVH

class CommentsAdapter(private val commentItemClickListener: CommentItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var mCommentItems = mutableListOf<Item>()

    private val collapseComments: (Int) -> Unit = {
        val collapse = !(mCommentItems[it] as Comment).isPartiallyCollapsed
        (mCommentItems[it] as Comment).isPartiallyCollapsed = collapse
        val depth = when(val item = mCommentItems[it]){
            is Comment -> item.depth ?: 0
            else -> 0
        }
        var i = it
        while(++i < itemCount - 1){
            val item = mCommentItems[i]
            val itemDepth = when(item){
                is Comment -> item.depth ?: 0
                is More -> item.depth
                else -> 0
            }
            if(itemDepth <= depth){
                break
            }
            item.hiddenPoints += if(collapse) 1 else -1
        }
        notifyItemRangeChanged(it, i - it)
    }


    fun submitList(items: List<Item>){
        mCommentItems = items.toMutableList()
        notifyDataSetChanged()
    }

    fun addItems(position: Int, items: List<Item>){
        mCommentItems.removeAt(position)
        notifyItemRemoved(position)
        mCommentItems.addAll(position, items)
        notifyItemRangeInserted(position, items.size)
        notifyItemRangeChanged(position + items.size, mCommentItems.size - (position + items.size + 1))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_more_comment -> MoreVH.create(parent)
            R.layout.item_comment -> CommentVH.create(parent)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(val commentItem = mCommentItems[position]){
            is Comment -> (holder as CommentVH).bind(commentItem, collapseComments)
            is More -> (holder as MoreVH).bind(commentItem) {
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

    interface CommentItemClickListener{
        fun onMoreCommentsClicked(position: Int, more: More)
        fun onContinueThreadClicked(more: More)
    }

}
