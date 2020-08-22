package dev.gtcl.reddit.ui.fragments.comments

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.CommentActions
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.ItemType
import dev.gtcl.reddit.models.reddit.listing.More
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.*
import io.noties.markwon.Markwon
import kotlin.math.max

class CommentsAdapter(private val markwon: Markwon, private val commentActions: CommentActions, private val itemClickListener: ItemClickListener, private val onViewAllClick: (() -> Unit)?) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    var allCommentsRetrieved: Boolean = onViewAllClick == null
        set(value){
            val previousValue = field
            field = value
            if(!previousValue && value){
                notifyItemRemoved(0)
            }
        }

    private var comments: MutableList<Item>? = null

    fun submitList(items: List<Item>?){
        val offset = if(allCommentsRetrieved) 0 else 1
        if(comments != null){
            notifyItemRangeRemoved(0 + offset, comments!!.size)
            comments = items?.toMutableList()
            notifyItemRangeInserted(0 + offset, max(items?.size ?: 1, 1))
        } else {
            comments = items?.toMutableList()
            if(comments.isNullOrEmpty()){
                notifyItemChanged(0 + offset)
            } else {
                notifyItemRemoved(0 + offset)
                notifyItemRangeInserted(0 + offset, max(items?.size ?: 1, 1))
            }
        }
    }

    fun addItems(position: Int, items: List<Item>){
        comments?.let {
            it.addAll(position, items)
            notifyItemRangeInserted(position, items.size)
        }
    }

    fun removeAt(position: Int){
        comments?.let {
            it.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun removeRange(position: Int, size: Int){
        for(i in 1..size){
            comments!!.removeAt(position)
        }
        notifyItemRangeRemoved(position, size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_view_all_comments -> ViewAllCommentsVH.create(parent)
            R.layout.item_more_comment -> MoreVH.create(parent)
            R.layout.item_comment -> CommentVH.create(parent)
            R.layout.item_network_state -> NetworkStateItemVH.create(parent)
            R.layout.item_no_items_found -> NoItemFoundVH.create(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val offset = if(allCommentsRetrieved) 0 else -1
        when(val viewType = getItemViewType(position)){
            R.layout.item_view_all_comments -> (holder as ViewAllCommentsVH).bind(onViewAllClick!!)
            R.layout.item_more_comment -> (holder as MoreVH).bind(comments!![position + offset] as More, itemClickListener)
            R.layout.item_comment -> (holder as CommentVH).bind(comments!![position + offset] as Comment, markwon, commentActions, itemClickListener)
            R.layout.item_network_state -> (holder as NetworkStateItemVH).bind(NetworkState.LOADING){}
            R.layout.item_no_items_found -> (holder as NoItemFoundVH).bind(ItemType.Comment)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val offset = if(allCommentsRetrieved) 0 else -1
        return when{
            position == 0 && !allCommentsRetrieved -> R.layout.item_view_all_comments
            comments == null -> R.layout.item_network_state
            comments?.isEmpty() ?: false -> R.layout.item_no_items_found
            comments?.get(position + offset) is More -> R.layout.item_more_comment
            comments?.get(position + offset) is Comment -> R.layout.item_comment
            else -> throw IllegalArgumentException("Unable to determine view type at position $position")
        }
    }

    override fun getItemCount(): Int{
        return if(!comments.isNullOrEmpty()){
               comments!!.size + if(!allCommentsRetrieved) 1 else 0
        } else {
            if(allCommentsRetrieved) 1 else 2
        }
    }

}
