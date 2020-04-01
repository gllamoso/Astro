package dev.gtcl.reddit.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.database.ReadListing
import dev.gtcl.reddit.listings.Comment
import dev.gtcl.reddit.listings.ListingItem
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.listings.Post
import dev.gtcl.reddit.ui.fragments.comments.CommentsAdapter
import dev.gtcl.reddit.ui.fragments.home.listing.NetworkStateItemViewHolder
import dev.gtcl.reddit.ui.fragments.home.listing.PostViewHolder
import java.io.InvalidObjectException

class ListingAdapter(private val retryCallback: () -> Unit, private val postActions: PostActions) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = ArrayList<ListingItem>()
    private var allReadSubs: HashSet<String> = HashSet()
    private var currentIds: HashSet<String> = HashSet()

    fun setReadSubs(list: List<ReadListing>){
        allReadSubs = list.map { it.name }.toHashSet()
    }

    fun loadInitial(items: List<ListingItem>){
        this.items.clear()
        this.items.addAll(items)
        currentIds = items.map { it.name }.toHashSet()
        notifyDataSetChanged()
    }

    fun loadMore(items: List<ListingItem>){
        val insertionPoint = this.items.size
        var itemSize = 0
        for(item: ListingItem in items){
            if(!currentIds.contains(item.name)){
                this.items.add(item)
                currentIds.add(item.name)
                itemSize++
            }
        }
        notifyItemRangeChanged(insertionPoint, itemSize)
    }

    private var networkState = NetworkState.LOADED
    fun setNetworkState(networkState: NetworkState){
        this.networkState = networkState
    }

    private fun hasNetworkStateView() = (networkState != NetworkState.LOADED)

    override fun getItemCount(): Int = items.size + if(hasNetworkStateView()) 1 else 0

    override fun getItemViewType(position: Int): Int {
        if(position >= items.size) return R.layout.item_network_state
        return when(val item = items[position]){
            is Post -> R.layout.item_post
            is Comment -> R.layout.item_comment
            else -> throw InvalidObjectException("Unexpected item found: ${item.javaClass.simpleName} in position $position" )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_post -> PostViewHolder.create(parent)
            R.layout.item_comment -> CommentsAdapter.CommentViewHolder.create(parent)
            R.layout.item_network_state -> NetworkStateItemViewHolder.create(parent, retryCallback)
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
                (holder as CommentsAdapter.CommentViewHolder).bind(comment) { _, c ->  }
            }
            R.layout.item_network_state -> (holder as NetworkStateItemViewHolder).bindTo(networkState)
        }
    }

}