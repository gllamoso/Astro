package dev.gtcl.reddit.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.database.ReadListing
import dev.gtcl.reddit.listings.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.fragments.comments.CommentsAdapter
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.SubredditActions
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.mine.MultiAndSubsListAdapter
import dev.gtcl.reddit.ui.viewholders.CommentViewHolder
import dev.gtcl.reddit.ui.viewholders.ListingViewHolder
import dev.gtcl.reddit.ui.viewholders.NetworkStateItemViewHolder
import dev.gtcl.reddit.ui.viewholders.PostViewHolder
import java.io.InvalidObjectException

class ListingAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = ArrayList<Item>()
    private var allReadSubs: HashSet<String> = HashSet()
    private var currentIds: HashSet<String> = HashSet()
    var lastItemReached = false

    lateinit var postActions: PostActions
    lateinit var retry: () -> Unit
    lateinit var onLastItemReached: () -> Unit

    constructor(postActions: PostActions, retry: () -> Unit, onLastItemReached: () -> Unit) : this() {
        this.postActions = postActions
        this.retry = retry
        this.onLastItemReached = onLastItemReached
    }

    lateinit var subredditActions: SubredditActions
    constructor(subredditActions: SubredditActions, retry: () -> Unit, onLastItemReached: () -> Unit): this(){
        this.subredditActions = subredditActions
        this.retry = retry
        this.onLastItemReached = onLastItemReached
    }

    private var networkState = NetworkState.LOADED
    fun setNetworkState(networkState: NetworkState){
        this.networkState = networkState
    }

    fun setReadSubs(list: List<ReadListing>){
        allReadSubs = list.map { it.name }.toHashSet()
    }

    fun loadInitial(items: List<Item>){
        this.items.clear()
        this.items.addAll(items)
        currentIds = items.map { it.name }.toHashSet()
        notifyDataSetChanged()
    }

    fun loadMore(items: List<Item>){
        val insertionPoint = this.items.size
        var itemSize = 0
        for(item: Item in items){
            if(!currentIds.contains(item.name)){
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
            is Subreddit -> R.layout.item_listing
            else -> throw InvalidObjectException("Unexpected item found: ${item.javaClass.simpleName} in position $position" )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_post -> PostViewHolder.create(parent)
            R.layout.item_comment -> CommentViewHolder.create(parent)
            R.layout.item_listing -> ListingViewHolder.create(parent)
            R.layout.item_network_state -> NetworkStateItemViewHolder.create(parent, retry)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_post -> {
                val post = items[position] as Post
                (holder as PostViewHolder).bind(post, postActions, allReadSubs.contains(post.name)) {
                    items.remove(post)
                    notifyItemRemoved(position)
                }
            }
            R.layout.item_comment -> {
                val comment = items[position] as Comment
                (holder as CommentViewHolder).bind(comment) { _, c ->  }
            }
            R.layout.item_listing -> {
                val subreddit = items[position] as Subreddit
                (holder as ListingViewHolder).bind(SubredditListing(subreddit), subredditActions)
            }
            R.layout.item_network_state -> (holder as NetworkStateItemViewHolder).bindTo(networkState)
        }
    }

}