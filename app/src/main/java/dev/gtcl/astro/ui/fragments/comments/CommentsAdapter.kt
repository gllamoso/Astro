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
import io.noties.markwon.Markwon
import kotlin.math.max

class CommentsAdapter(
    private val markwon: Markwon,
    private val commentActions: CommentActions,
    private val itemClickListener: ItemClickListener,
    private val userId: String?,
    allCommentsFetched: Boolean,
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

    private var comments: MutableList<Item>? = null

    fun submitList(items: List<Item>?) {
        val offset = if (allCommentsFetched) 0 else 1
        if (!comments.isNullOrEmpty()) {
            notifyItemRangeRemoved(offset, comments!!.size)
            comments = items?.toMutableList()
            notifyItemRangeInserted(offset, max(items?.size ?: 1, 1))
        } else {
            comments = items?.toMutableList()
            notifyItemRemoved(offset)
            notifyItemRangeInserted(offset, max(items?.size ?: 1, 1))
        }
    }

    fun addItems(position: Int, items: List<Item>) {
        val offset = if (allCommentsFetched) 0 else 1
        if (comments.isNullOrEmpty()) {
            notifyItemRemoved(offset)
        }
        comments?.let {
            it.addAll(position - offset, items)
            notifyItemRangeInserted(position, items.size)
        }
    }

    fun removeAt(position: Int) {
        comments?.let {
            it.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun removeRange(position: Int, size: Int) {
        val offset = if (allCommentsFetched) 0 else 1
        for (i in 1..size) {
            comments!!.removeAt(position - offset)
        }
        notifyItemRangeRemoved(position, size)
    }

    fun updateAt(item: Item, position: Int) {
        val offset = if (allCommentsFetched) 0 else 1
        comments?.let {
            it[position] = item
            notifyItemChanged(position - offset)
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
        val offset = if (allCommentsFetched) 0 else -1
        when (val viewType = getItemViewType(position)) {
            R.layout.item_view_all_comments -> (holder as ViewAllCommentsVH).bind(onViewAllClick)
            R.layout.item_more_comment -> (holder as MoreVH).bind(
                comments!![position + offset] as More,
                itemClickListener
            )
            R.layout.item_comment -> (holder as CommentVH).bind(
                comments!![position + offset] as Comment,
                markwon,
                commentActions,
                userId,
                itemClickListener
            )
            R.layout.item_network_state -> (holder as NetworkStateItemVH).bind(NetworkState.LOADING) {}
            R.layout.item_no_items_found -> (holder as NoItemFoundVH).bind(ItemType.Comment)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        val offset = if (allCommentsFetched) 0 else -1
        return when {
            position == 0 && !allCommentsFetched -> R.layout.item_view_all_comments
            comments == null -> R.layout.item_network_state
            comments?.isEmpty() ?: false -> R.layout.item_no_items_found
            comments?.get(position + offset) is More -> R.layout.item_more_comment
            comments?.get(position + offset) is Comment -> R.layout.item_comment
            else -> throw IllegalArgumentException("Unable to determine view type at position $position")
        }
    }

    override fun getItemCount(): Int {
        return if (!comments.isNullOrEmpty()) {
            comments!!.size + if (!allCommentsFetched) 1 else 0
        } else {
            if (allCommentsFetched) 1 else 2
        }
    }

}
