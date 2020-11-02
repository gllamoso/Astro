package dev.gtcl.astro.ui.fragments.comments

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.R
import dev.gtcl.astro.actions.CommentActions
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.models.reddit.listing.Comment
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.ItemType
import dev.gtcl.astro.models.reddit.listing.More
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.ui.viewholders.*
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import kotlin.math.max

class CommentsAdapter(
    private val commentActions: CommentActions,
    private val itemClickListener: ItemClickListener,
    private val userId: String?,
    allCommentsFetched: Boolean,
    private val movementMethod: BetterLinkMovementMethod,
    private val onViewAllClick: (() -> Unit)
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var allCommentsFetched: Boolean = allCommentsFetched
        set(value) {
            val previousValue = field
            field = value
            if (previousValue != value) {
                if (value) {
                    notifyItemRemoved(0)
                } else {
                    notifyItemInserted(0)
                }
            }
        }

    fun getOffset(): Int = if(allCommentsFetched) 0 else 1

    private var comments: MutableList<Item>? = null

    fun submitList(items: List<Item>?) {
        val offset = if (allCommentsFetched) 0 else 1
        if (!comments.isNullOrEmpty()) {
            notifyItemRangeRemoved(offset, (comments ?: return).size)
            comments = items?.toMutableList()
            notifyItemRangeInserted(offset, max(items?.size ?: 1, 1))
        } else {
            comments = items?.toMutableList()
            notifyItemRemoved(offset)
            notifyItemRangeInserted(offset, max(items?.size ?: 1, 1))
        }
    }

    fun addItems(position: Int, items: List<Item>) {
        comments?.let {
            it.addAll(position, items)
            notifyItemRangeInserted(position + getOffset(), items.size)
        }
    }

    fun removeItemAt(position: Int) {
        comments?.let {
            it.removeAt(position)
            notifyItemRemoved(position + getOffset())
        }
    }

    fun removeItems(position: Int, size: Int) {
        for (i in 1..size) {
            (comments ?: return).removeAt(position)
        }
        notifyItemRangeRemoved(position + getOffset(), size)
    }

    fun updateItemAt(item: Item, position: Int) {
        comments?.let {
            it[position] = item
            notifyItemChanged(position + getOffset())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_view_all_comments -> ViewAllCommentsVH.create(parent)
            R.layout.item_more_comment -> MoreVH.create(parent)
            R.layout.item_comment -> CommentVH.create(parent)
            R.layout.item_network_state -> NetworkStateItemVH.create(parent)
            R.layout.item_no_items_found -> NoItemFoundVH.create(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val viewType = getItemViewType(position)) {
            R.layout.item_view_all_comments -> (holder as ViewAllCommentsVH).bind(onViewAllClick)
            R.layout.item_more_comment -> (holder as MoreVH).bind(
                (comments ?: return)[position - getOffset()] as More,
                itemClickListener
            )
            R.layout.item_comment -> (holder as CommentVH).bind(
                (comments ?: return)[position - getOffset()] as Comment,
                movementMethod,
                commentActions,
                userId,
                    (itemCount - 1) == position,
                itemClickListener
            )
            R.layout.item_network_state -> (holder as NetworkStateItemVH).bind(NetworkState.LOADING) {}
            R.layout.item_no_items_found -> (holder as NoItemFoundVH).bind(ItemType.Comment)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == 0 && !allCommentsFetched -> R.layout.item_view_all_comments
            comments == null -> R.layout.item_network_state
            comments?.isEmpty() ?: false -> R.layout.item_no_items_found
            comments?.get(position - getOffset()) is More -> R.layout.item_more_comment
            comments?.get(position - getOffset()) is Comment -> R.layout.item_comment
            else -> throw IllegalArgumentException("Unable to determine view type at position $position")
        }
    }

    override fun getItemCount(): Int {
        return if (!comments.isNullOrEmpty()) {
            comments!!.size + getOffset()
        } else {
            if (allCommentsFetched) 1 else 2
        }
    }

}
