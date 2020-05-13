package dev.gtcl.reddit.ui.fragments.dialog.subreddits.trending

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.NetworkStateItemViewHolder
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.ui.viewholders.TrendingSubredditViewHolder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class TrendingAdapter(private val subredditActions: SubredditActions, private val retry: () -> Unit, private val onLastItemReached: () -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = ArrayList<TrendingSubredditPost>()
    private var currentIds: HashSet<String> = HashSet()
    private var subscribedSubs: HashSet<String> = HashSet()
    private var favoriteSubs: HashSet<String> = HashSet()
    private var lastItemReached = false
    private var networkState = NetworkState.LOADED

    fun setNetworkState(networkState: NetworkState){
        this.networkState = networkState
    }

    fun setSubscribedSubs(subs: List<Subreddit>) {
        subscribedSubs = subs.map { it.displayName.toLowerCase(Locale.ROOT) }.toHashSet()
        favoriteSubs = subs.filter { it.isFavorite }.map { it.displayName.toLowerCase(Locale.ROOT)}.toHashSet()
        for(item: TrendingSubredditPost in items){
            item.setSubscribedTo(subscribedSubs)
            item.setFavorites(favoriteSubs)
        }
        notifyDataSetChanged()
    }

    fun loadInitial(items: List<TrendingSubredditPost>){
        this.items.clear()
        for(post: TrendingSubredditPost in items){
            post.setSubscribedTo(subscribedSubs)
            post.setFavorites(favoriteSubs)
        }
        this.items.addAll(items)
        currentIds = items.map { it.post.name }.toHashSet()
        notifyDataSetChanged()
    }

    fun loadMore(items: List<TrendingSubredditPost>){
        val insertionPoint = this.items.size
        var itemSize = 0
        for(item: TrendingSubredditPost in items){
            if(!currentIds.contains(item.post.name)){
                item.setSubscribedTo(subscribedSubs)
                item.setFavorites(favoriteSubs)
                this.items.add(item)
                currentIds.add(item.post.name)
                itemSize++
            }
        }
        if(itemSize == 0) {
            onLastItemReached()
            lastItemReached = true
            notifyItemRemoved(this.items.size)
            return
        }
        notifyItemRangeInserted(insertionPoint, itemSize)
    }

    private fun hasNetworkStateView() = (networkState != NetworkState.LOADED)

    override fun getItemCount(): Int = items.size + if(hasNetworkStateView() && !lastItemReached) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if(position >= items.size) R.layout.item_network_state
        else R.layout.item_trending_subreddit
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            R.layout.item_trending_subreddit -> (holder as TrendingSubredditViewHolder).bind(items[position], subredditActions)
            R.layout.item_network_state -> (holder as NetworkStateItemViewHolder).bindTo(networkState)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_trending_subreddit -> TrendingSubredditViewHolder.create(parent)
            R.layout.item_network_state -> NetworkStateItemViewHolder.create(parent, retry)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

}
