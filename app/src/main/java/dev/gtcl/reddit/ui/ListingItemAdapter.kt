package dev.gtcl.reddit.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.*
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.*
import java.io.InvalidObjectException

class ListingItemAdapter(
    private val postActions: PostActions? = null,
    private val subredditActions: SubredditActions? = null,
    private val messageActions: MessageActions? = null,
    private val commentActions: CommentActions? = null,
    private val itemClickListener: ItemClickListener,
    private val retry: () -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemClickListener  {

    var networkState: NetworkState? = NetworkState.LOADING
        set(value){
            val addNetworkStateView = (value != NetworkState.LOADED && value != networkState)
            field = value
            if(addNetworkStateView){
                notifyItemInserted(items.size)
            }
        }

    private var items = ArrayList<Item>()

    fun clearItems(){
        val itemSize = items.size
        items.clear()
        notifyItemRangeRemoved(0, itemSize)
    }

    fun setItems(items: List<Item>){
        val previousSize = this.items.size
        notifyItemRemoved(previousSize)
        this.items = ArrayList(items)
        if(previousSize < this.items.size){
            notifyItemRangeInserted(previousSize, this.items.size - previousSize)
        } else {
            notifyDataSetChanged()
        }
    }

    fun updateItem(item: Item, position: Int){
        items[position] = item
        notifyItemChanged(position)
    }

    private val isLoading: Boolean
        get() = networkState != NetworkState.LOADED

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

    private val hideItem: (Int) -> Unit = {
        items.removeAt(it)
        notifyItemRemoved(it)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_post -> {
                if(postActions == null){
                    throw IllegalStateException("Post Actions not initialized")
                }
                val post = items[position] as Post
                val postClicked: (Post, Int) -> Unit = { p, adapterPosition ->
                    itemClicked(p, adapterPosition)
                }
                (holder as PostVH).bind(post, postActions,
                    hideAction = hideItem,
                    postClicked = postClicked)
            }
            R.layout.item_comment -> {
                val comment = items[position] as Comment
                if(commentActions == null){
                    throw IllegalStateException("Comment Actions not initialized")
                }
                (holder as CommentVH).bind(comment, commentActions, itemClickListener)
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
                (holder as MessageVH).bind(message, messageActions)
            }
            R.layout.item_network_state -> (holder as NetworkStateItemVH).bindTo(networkState)
        }
    }

    override fun itemClicked(item: Item, position: Int) {
        itemClickListener.itemClicked(item, position)
    }

}