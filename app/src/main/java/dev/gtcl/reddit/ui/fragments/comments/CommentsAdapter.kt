package dev.gtcl.reddit.ui.fragments.comments

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.CommentActions
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.More
import dev.gtcl.reddit.ui.viewholders.CommentVH
import dev.gtcl.reddit.ui.viewholders.MoreVH
import io.noties.markwon.Markwon

class CommentsAdapter(private val markwon: Markwon, private val commentActions: CommentActions, private val itemClickListener: ItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemClickListener{

    private var mCommentItems = mutableListOf<Item>()

    fun submitList(items: List<Item>){
        mCommentItems = items.toMutableList()
        notifyDataSetChanged()
    }

    fun addItems(position: Int, items: List<Item>){
        val itemAtPosition = mCommentItems[position]
        if (itemAtPosition is More && itemAtPosition.isChildQueueEmpty()){
            mCommentItems.removeAt(position)
            notifyItemRemoved(position)
        }

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
            is Comment -> (holder as CommentVH).bind(commentItem, markwon, commentActions, this)
            is More -> (holder as MoreVH).bind(commentItem, this)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(mCommentItems[position] is More)
                R.layout.item_more_comment
            else
                R.layout.item_comment
    }

    override fun getItemCount(): Int = mCommentItems.size

    override fun itemClicked(item: Item, position: Int) {
        val itemInPosition = mCommentItems[position]
        if(itemInPosition is Comment){
            val collapse = !(mCommentItems[position] as Comment).isPartiallyCollapsed
            (mCommentItems[position] as Comment).isPartiallyCollapsed = collapse
            val depth = itemInPosition.depth ?: 0
            var i = position
            while(++i < itemCount - 1){
                val currItem = mCommentItems[i]
                val itemDepth = when(currItem){
                    is Comment -> currItem.depth ?: 0
                    is More -> currItem.depth
                    else -> 0
                }
                if(itemDepth <= depth){
                    break
                }
                currItem.hiddenPoints += if(collapse){
                    1
                } else {
                    -1
                }
            }
            notifyItemRangeChanged(position, i - position)
        } else if(itemInPosition is More) {
            itemClickListener.itemClicked(item, position)
        }
    }

}
