package dev.gtcl.reddit.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.MessageActions
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.*
import java.io.InvalidObjectException

class ListingItemAdapter(
    private val postActions: PostActions? = null,
    private val subredditActions: SubredditActions? = null,
    private val messageActions: MessageActions? = null,
    private val retry: () -> Unit,
    private val hideableItems: Boolean = false): RecyclerView.Adapter<RecyclerView.ViewHolder>(), ItemClickListener  {

    var itemClickListener: ItemClickListener? = null

    var networkState = NetworkState.LOADING
        set(value){
            val addNetworkStateView = (value != NetworkState.LOADED && value != networkState)
            field = value
            if(addNetworkStateView){
                notifyItemInserted(items.size)
            }
        }

    private val items = ArrayList<Item>()

    fun clearItems(){
        items.clear()
        notifyDataSetChanged()
    }

    fun addItems(newItems: List<Item>){
        val insertionPoint = items.size
        notifyItemRemoved(insertionPoint)
        if(newItems.isNotEmpty()){
            items.addAll(newItems)
            notifyItemRangeInserted(insertionPoint, newItems.size)
        }
    }

    fun updateFavoriteItems(favoriteSet: HashSet<String>){
        for(item: Item in items){
            if(item is Subreddit){
                item.isFavorite = favoriteSet.contains(item.displayName)
            }
        }
        notifyDataSetChanged()
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
            R.layout.item_post -> PostViewHolder.create(parent)
            R.layout.item_comment -> CommentViewHolder.create(parent)
            R.layout.item_subreddit -> SubredditViewHolder.create(parent)
            R.layout.item_message -> MessageViewHolder.create(parent)
            R.layout.item_network_state -> NetworkStateItemViewHolder.create(parent, retry)
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
                val hide: (() -> Unit)? = if(hideableItems) {
                    {   
                        items.remove(post)
                        notifyItemRemoved(position)
                    }
                } else {
                    null
                }
                val postClicked: (Post) -> Unit = {
                    itemClicked(it)
                }
                (holder as PostViewHolder).bind(post, postActions,
                    hideAction = hide,
                    postClicked = postClicked)
            }
            R.layout.item_comment -> {
                val comment = items[position] as Comment
                (holder as CommentViewHolder).bind(comment) { _, _ ->  }
            }
            R.layout.item_subreddit -> {
                val subreddit = items[position] as Subreddit
                if(subredditActions == null){
                    throw IllegalStateException("Subreddit Actions not initialized")
                }
                (holder as SubredditViewHolder).bind(subreddit, subredditActions, null) {itemClicked(subreddit)}
            }
            R.layout.item_message -> {
                val message = items[position] as Message
                if(messageActions == null){
                    throw java.lang.IllegalStateException("Message Actions not initialized")
                }
                (holder as MessageViewHolder).bind(message, messageActions)
            }
            R.layout.item_network_state -> (holder as NetworkStateItemViewHolder).bindTo(networkState)
        }
    }

    override fun itemClicked(item: Item) {
        itemClickListener?.itemClicked(item)
    }

}