package dev.gtcl.reddit.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.MessageActions
import dev.gtcl.reddit.database.ItemRead
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.ui.viewholders.*
import java.io.InvalidObjectException

class ListingAdapter(
    private val postActions: PostActions? = null,
    private val subredditActions: SubredditActions? = null,
    private val messageActions: MessageActions? = null,
    private val retry: () -> Unit,
    private val onLastItemReached: () -> Unit): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = ArrayList<Item>()
    private var allReadSubs: HashSet<String> = HashSet()
    private var currentIds: HashSet<String> = HashSet()
    var lastItemReached = false
    private var subscribedSubs: HashSet<String> = HashSet()
    private var favSubs: HashSet<String> = HashSet()

    fun setSubscribedSubs(subs: List<Subreddit>){
        subscribedSubs = subs.map { it.displayName }.toHashSet()
        favSubs = subs.filter { it.isFavorite }.map { it.displayName}.toHashSet()
        for(item: Item in items){
            if(item is Subreddit) {
                item.apply {
                    userSubscribed = subscribedSubs.contains(displayName)
                    isFavorite = favSubs.contains(displayName)
                }
            }
        }
        notifyDataSetChanged()
    }

    private var networkState = NetworkState.LOADED
    fun setNetworkState(networkState: NetworkState){
        this.networkState = networkState
    }

    fun setReadSubs(list: List<ItemRead>){
        allReadSubs = list.map { it.name }.toHashSet()
    }

    fun loadInitial(items: List<Item>){
        this.items.clear()
        for(item: Item in items){
            if(item is Subreddit){
                item.apply {
                    userSubscribed = subscribedSubs.contains(displayName)
                    isFavorite = favSubs.contains(displayName)
                }
            }
        }
        this.items.addAll(items)
        currentIds = items.map { it.name }.toHashSet()
        notifyDataSetChanged()
    }

    fun loadMore(items: List<Item>){
        val insertionPoint = this.items.size
        var itemSize = 0
        for(item: Item in items){
            if(!currentIds.contains(item.name)){
                if(item is Subreddit){
                    item.apply {
                        userSubscribed = subscribedSubs.contains(displayName)
                        isFavorite = favSubs.contains(displayName)
                    }
                }
                this.items.add(item)
                currentIds.add(item.name)
                itemSize++
            }
        }
        if(itemSize == 0) {
            onLastItemReached()
            lastItemReached = true
            notifyItemRemoved(this.items.size)
            return
        }
        notifyItemRangeChanged(insertionPoint, itemSize)
    }

    private fun hasNetworkStateView() = (networkState != NetworkState.LOADED)

    override fun getItemCount(): Int = items.size + if(hasNetworkStateView() && !lastItemReached) 1 else 0

    override fun getItemViewType(position: Int): Int {
        if(position >= items.size) return R.layout.item_network_state
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
                (holder as PostVH).bind(post, postActions, hideAction = {
                    items.remove(post)
                    notifyItemRemoved(position)
                },
                    postClicked = {
                        items.remove(post)
                        notifyItemRemoved(position)
                    }
                )
            }
            R.layout.item_comment -> {
                val comment = items[position] as Comment
                (holder as CommentVH).bind(comment) { _, _ ->  }
            }
            R.layout.item_subreddit -> {
                val subreddit = items[position] as Subreddit
                if(subredditActions == null){
                    throw IllegalStateException("Subreddit Actions not initialized")
                }
//                (holder as SubredditViewHolder).bind(subreddit, subredditActions, null)
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

}