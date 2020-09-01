package dev.gtcl.astro.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.R
import dev.gtcl.astro.actions.*
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.network.Status
import dev.gtcl.astro.ui.viewholders.*
import io.noties.markwon.Markwon
import java.io.InvalidObjectException

class ListingItemAdapter(
    private val markwon: Markwon?,
    private val postActions: PostActions? = null,
    private val subredditActions: SubredditActions? = null,
    private val messageActions: MessageActions? = null,
    private val commentActions: CommentActions? = null,
    private val expected: ItemType? = null,
    var blurNsfw: Boolean = false,
    private val itemClickListener: ItemClickListener,
    private val username: String?,
    private val retry: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: MutableList<Item>? = null

    var networkState: NetworkState = NetworkState.LOADING
        set(value){
            val loading = (networkState == NetworkState.LOADED && (value.status == Status.FAILED || value.status == Status.RUNNING))
            val error = ((networkState == NetworkState.LOADING && value.status == Status.FAILED) || (networkState.status == Status.FAILED && value == NetworkState.LOADING))
            field = value
            if(loading){
                if(items?.size ?: 0 == 0){
                    notifyItemChanged(0)
                } else {
                    notifyItemInserted(items?.size ?: 0)
                }
            }
            if(error){
                notifyItemChanged(items?.size ?: 0)
            }
        }

    fun submitList(items: List<Item>?) {
        notifyItemRangeRemoved(0, itemCount)
        this.items = items?.toMutableList()
        if(items.isNullOrEmpty()){
            notifyItemChanged(0)
        } else {
            notifyItemRangeInserted(0, items.size)
        }
    }

    fun addItems(items: List<Item>) {
        val previousSize = this.items?.size ?: 0
        notifyItemRemoved(previousSize)

        if (this.items == null) {
            this.items = mutableListOf()
        }
        this.items!!.addAll(items)
        notifyItemRangeInserted(previousSize, items.size)
    }

    fun updateAt(position: Int, item: Item){
        items?.let {
            it[position] = item
            notifyItemChanged(position)
        }
    }

    fun removeAt(position: Int) {
        items?.let {
            it.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount(): Int{
        return if(items.isNullOrEmpty()){
            1
        } else {
            items!!.size + if (networkState.status == Status.RUNNING || networkState.status == Status.FAILED) 1 else 0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when{
            items.isNullOrEmpty() -> {
                if(items?.isEmpty() == true && networkState == NetworkState.LOADED)  {
                    R.layout.item_no_items_found
                } else {
                    R.layout.item_network_state
                }
            }
            items != null && position == items!!.size -> R.layout.item_network_state
            else -> {
                when (val item = items!![position]) {
                    is Post -> R.layout.item_post
                    is Comment -> R.layout.item_comment_detailed
                    is Subreddit -> R.layout.item_subreddit
                    is Message -> R.layout.item_message
                    else -> throw InvalidObjectException("Unexpected item found: ${item.javaClass.simpleName} in position $position")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_post -> PostVH.create(parent)
            R.layout.item_comment_detailed -> CommentDetailedVH.create(parent)
            R.layout.item_subreddit -> SubredditVH.create(parent)
            R.layout.item_message -> MessageVH.create(parent)
            R.layout.item_network_state -> NetworkStateItemVH.create(parent)
            R.layout.item_no_items_found -> NoItemFoundVH.create(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_post -> {
                if (postActions == null) {
                    throw IllegalStateException("Post Actions not initialized")
                }
                val post = items!![position] as Post
                (holder as PostVH).bind(post, postActions, blurNsfw, username, itemClickListener)
            }
            R.layout.item_comment_detailed -> {
                val comment = items!![position] as Comment
                if (commentActions == null) {
                    throw IllegalStateException("Comment Actions not initialized")
                }
                (holder as CommentDetailedVH).bind(comment, markwon, commentActions, username, itemClickListener)
            }
            R.layout.item_subreddit -> {
                val subreddit = items!![position] as Subreddit
                if (subredditActions == null) {
                    throw IllegalStateException("Subreddit Actions not initialized")
                }
                (holder as SubredditVH).bind(subreddit, subredditActions, itemClickListener)
            }
            R.layout.item_message -> {
                val message = items!![position] as Message
                if (messageActions == null) {
                    throw java.lang.IllegalStateException("Message Actions not initialized")
                }
                (holder as MessageVH).bind(message, markwon, messageActions, username, itemClickListener)
            }
            R.layout.item_network_state -> (holder as NetworkStateItemVH).bind(networkState, retry)
            R.layout.item_no_items_found -> (holder as NoItemFoundVH).bind(expected)
        }
    }

}