package dev.gtcl.reddit.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.*
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.*
import io.noties.markwon.Markwon
import java.io.InvalidObjectException

class ListingItemAdapter(
    private val markwon: Markwon,
    private val postActions: PostActions? = null,
    private val subredditActions: SubredditActions? = null,
    private val messageActions: MessageActions? = null,
    private val commentActions: CommentActions? = null,
    private val itemClickListener: ItemClickListener,
    private val retry: () -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    var networkState: NetworkState? = NetworkState.LOADING
        set(value){
            val addNetworkStateView = (value != NetworkState.LOADED && value != networkState)
            field = value
            if(addNetworkStateView){
                notifyItemInserted(items.size)
            }
        }

    private val isLoading: Boolean
        get() = networkState != NetworkState.LOADED

    private var items = mutableListOf<Item>()

    fun clearItems(){
        val itemSize = items.size
        items.clear()
        notifyItemRangeRemoved(0, itemSize)
    }

    fun submitList(items: List<Item>){
        val previousSize = this.items.size
        notifyItemRemoved(previousSize)
        this.items = items.toMutableList()
        notifyDataSetChanged()
    }

    fun addItems(items: List<Item>){
        val previousSize = this.items.size
        notifyItemRemoved(previousSize)
        this.items.addAll(items)
        notifyItemRangeInserted(previousSize, items.size)
    }

    fun addItems(position: Int, items: List<Item>){
        this.items.addAll(position, items)
        notifyItemRangeInserted(position, items.size)
    }

    fun update(item: Item, position: Int){
        items[position] = item
        notifyItemChanged(position)
    }

    fun removeAt(position: Int){
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getItemCount(): Int = items.size + if(isLoading) 1 else 0

    override fun getItemViewType(position: Int): Int {
        if(position >= items.size){
            return R.layout.item_network_state
        }
        return when(val item = items[position]){
            is Post -> R.layout.item_post
            is Comment -> R.layout.item_comment
            is Subreddit -> R.layout.item_subreddit
            is Message -> R.layout.item_message
            else -> throw InvalidObjectException("Unexpected item found: ${item.javaClass.simpleName} in position $position" )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_post -> PostVH.create(parent)
            R.layout.item_comment -> CommentVH.create(parent)
            R.layout.item_subreddit -> SubredditVH.create(parent)
            R.layout.item_message -> MessageVH.create(parent)
            R.layout.item_network_state -> NetworkStateItemVH.create(parent, retry)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_post -> {
                if(postActions == null){
                    throw IllegalStateException("Post Actions not initialized")
                }
                val post = items[position] as Post
                (holder as PostVH).bind(post, postActions, itemClickListener)
            }
            R.layout.item_comment -> {
                val comment = items[position] as Comment
                if(commentActions == null){
                    throw IllegalStateException("Comment Actions not initialized")
                }
                (holder as CommentVH).bind(comment, markwon, commentActions, itemClickListener)
            }
            R.layout.item_subreddit -> {
                val subreddit = items[position] as Subreddit
                if(subredditActions == null){
                    throw IllegalStateException("Subreddit Actions not initialized")
                }
                (holder as SubredditVH).bind(subreddit, subredditActions, itemClickListener)
            }
            R.layout.item_message -> {
                val message = items[position] as Message
                if(messageActions == null){
                    throw java.lang.IllegalStateException("Message Actions not initialized")
                }
                (holder as MessageVH).bind(message, messageActions, itemClickListener)
            }
            R.layout.item_network_state -> (holder as NetworkStateItemVH).bindTo(networkState)
        }
    }

}