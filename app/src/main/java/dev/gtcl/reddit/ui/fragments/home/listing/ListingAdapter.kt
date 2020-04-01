package dev.gtcl.reddit.ui.fragments.home.listing

import android.util.Log
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.database.ReadListing
import dev.gtcl.reddit.listings.Comment
import dev.gtcl.reddit.listings.ListingItem
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.listings.Post
import dev.gtcl.reddit.ui.PostActions
import dev.gtcl.reddit.ui.fragments.comments.CommentsAdapter
import java.io.InvalidObjectException

class ListingAdapter(private val retryCallback: () -> Unit, private val postActions: PostActions): PagedListAdapter<ListingItem, RecyclerView.ViewHolder>(LISTING_COMPARATOR){

    private var networkState: NetworkState? = null
    private var allReadSubs: HashSet<String> = HashSet()

    fun setReadSubs(list: List<ReadListing>){
        allReadSubs = list.map { it.name }.toHashSet()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.item_post -> {
                val post = getItem(position) as Post
                (holder as PostViewHolder).bind(post, postActions, allReadSubs.contains(post.name)) {
//                    currentList?.removeAt(position) // TODO: Remove
//                    notifyItemRemoved(position)
                    Log.d("TAE", "Current list data source class: ${currentList?.dataSource?.javaClass?.simpleName}")
                }
            }
            R.layout.item_comment -> {
                val comment = getItem(position) as Comment
                (holder as CommentsAdapter.CommentViewHolder).bind(comment) { _, c ->  }
            }
            R.layout.item_network_state -> (holder as NetworkStateItemViewHolder).bindTo(networkState)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            when(val item = getItem(position)){
                is Post -> (holder as PostViewHolder).bind(item, postActions, if(payloads[0] == true) true else allReadSubs.contains(item.name)) {
//                    currentList?.removeAt(position) // TODO: Remove
//                    notifyItemRemoved(position)
                }
                is Comment -> (holder as CommentsAdapter.CommentViewHolder).bind(item) { _, comment ->  }
            }
        }
        else onBindViewHolder(holder, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_post -> PostViewHolder.create(parent)
            R.layout.item_comment -> CommentsAdapter.CommentViewHolder.create(parent)
            R.layout.item_network_state -> NetworkStateItemViewHolder.create(parent, retryCallback)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

//  If network hasn't loaded yet
    private fun hasExtraRow() = networkState != null && networkState != NetworkState.LOADED

    override fun getItemViewType(position: Int): Int {
        if((hasExtraRow() && position == itemCount - 1) ) return  R.layout.item_network_state
        return when (val item = getItem(position)) {
            is Post -> R.layout.item_post
            is Comment -> R.layout.item_comment
            else -> throw InvalidObjectException("Unexpected item found: ${item?.javaClass?.simpleName} in position $position" )
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (hasExtraRow()) 1 else 0
    }

    fun setNetworkState(newNetworkState: NetworkState?) {
        val previousState = this.networkState
        val hadExtraRow = hasExtraRow()
        this.networkState = newNetworkState
        val hasExtraRow = hasExtraRow()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) notifyItemRemoved(super.getItemCount())
            else notifyItemInserted(super.getItemCount())
        } else if (hasExtraRow && previousState != newNetworkState)
            notifyItemChanged(itemCount - 1)
    }


    companion object {
        private val PAYLOAD_SCORE = Any()
        val POST_COMPARATOR = object : DiffUtil.ItemCallback<Post>() {
            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean =
                oldItem == newItem

            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean =
                oldItem.name == newItem.name

            override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
                return if (sameExceptScore(oldItem, newItem)) {
                    PAYLOAD_SCORE
                } else {
                    null
                }
            }
        }

        private fun sameExceptScore(oldItem: Post, newItem: Post): Boolean {
            // DON'T do this copy in a real app, it is just convenient here for the demo :)
            // because reddit randomizes scores, we want to pass it as a payload to minimize
            // UI updates between refreshes
            return oldItem.copy(score = newItem.score) == newItem
        }

        val LISTING_COMPARATOR = object : DiffUtil.ItemCallback<ListingItem>() {
            override fun areItemsTheSame(oldItem: ListingItem, newItem: ListingItem): Boolean  =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: ListingItem, newItem: ListingItem): Boolean =
                oldItem.name == newItem.name

            override fun getChangePayload(oldItem: ListingItem, newItem: ListingItem): Any? {
                if(oldItem is Post && newItem is Post)
                    return sameExceptScore(oldItem, newItem)
                return false
            }
        }
    }
}