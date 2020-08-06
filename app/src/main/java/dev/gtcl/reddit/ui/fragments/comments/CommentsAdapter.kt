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
import dev.gtcl.reddit.ui.viewholders.CommentVH
import dev.gtcl.reddit.ui.viewholders.MoreVH
import dev.gtcl.reddit.ui.viewholders.NetworkStateItemVH
import dev.gtcl.reddit.ui.viewholders.NoItemFoundVH
import io.noties.markwon.Markwon
import kotlin.math.max

class CommentsAdapter(private val markwon: Markwon, private val commentActions: CommentActions, private val itemClickListener: ItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var comments: MutableList<Item>? = null

    fun submitList(items: List<Item>?){
        if(comments != null){
            notifyItemRangeRemoved(0, comments!!.size)
            comments = items?.toMutableList()
            notifyItemRangeInserted(0, max(items?.size ?: 1, 1))
        } else {
            comments = items?.toMutableList()
            if(comments.isNullOrEmpty()){
                notifyItemChanged(0)
            } else {
                notifyItemRemoved(0)
                notifyItemRangeInserted(0, max(items?.size ?: 1, 1))
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
            R.layout.item_more_comment -> MoreVH.create(parent)
            R.layout.item_comment -> CommentVH.create(parent)
            R.layout.item_network_state -> NetworkStateItemVH.create(parent)
            R.layout.item_no_items_found -> NoItemFoundVH.create(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(val viewType = getItemViewType(position)){
            R.layout.item_more_comment -> (holder as MoreVH).bind(comments!![position] as More, itemClickListener)
            R.layout.item_comment -> (holder as CommentVH).bind(comments!![position] as Comment, markwon, commentActions, itemClickListener)
            R.layout.item_network_state -> (holder as NetworkStateItemVH).bind(NetworkState.LOADING){}
            R.layout.item_no_items_found -> (holder as NoItemFoundVH).bind(ItemType.Comment)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when{
            comments == null -> R.layout.item_network_state
            comments?.isEmpty() ?: false -> R.layout.item_no_items_found
            comments?.get(position) is More -> R.layout.item_more_comment
            comments?.get(position) is Comment -> R.layout.item_comment
            else -> throw IllegalArgumentException("Unable to determine view type at position $position")
        }
    }

    override fun getItemCount(): Int = max(comments?.size ?: 1, 1)

}
