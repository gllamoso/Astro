package dev.gtcl.reddit.ui.fragments.subreddits.trending

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.NetworkStateItemVH
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.ui.viewholders.TrendingSubredditVH
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class TrendingAdapter(private val itemClickListener: ItemClickListener, private val subredditActions: SubredditActions, private val retry: () -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = ArrayList<TrendingSubredditPost>()

    var networkState = NetworkState.LOADING
        set(value){
            val addNetworkStateView = (value != NetworkState.LOADED && value != networkState)
            field = value
            if(addNetworkStateView){
                notifyItemInserted(items.size)
            }
        }

    fun clearItems(){
        items.clear()
        notifyDataSetChanged()
    }

    fun addItems(trendingPosts: List<TrendingSubredditPost>){
        val insertionPoint = items.size
        notifyItemRemoved(insertionPoint)
        if(trendingPosts.isNotEmpty()){
            items.addAll(trendingPosts)
            notifyItemRangeInserted(insertionPoint, trendingPosts.size)
        }
    }

    fun updateFavoriteItems(ids: HashSet<String>){
        for(item: TrendingSubredditPost in items){
            item.setFavorites(ids)
        }
        notifyDataSetChanged()
    }

    fun updateSubscribedItems(ids: HashSet<String>){
        for(item: TrendingSubredditPost in items){
            item.setSubscribedTo(ids)
        }
        notifyDataSetChanged()
    }

    private val isLoading: Boolean
        get() = networkState != NetworkState.LOADED

    override fun getItemCount(): Int = items.size + if(isLoading) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if(position >= items.size) R.layout.item_network_state
        else R.layout.item_trending_subreddit
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            R.layout.item_trending_subreddit -> (holder as TrendingSubredditVH).bind(items[position], subredditActions) { itemClickListener.itemClicked(it) }
            R.layout.item_network_state -> (holder as NetworkStateItemVH).bindTo(networkState)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_trending_subreddit -> TrendingSubredditVH.create(parent)
            R.layout.item_network_state -> NetworkStateItemVH.create(parent, retry)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

}
