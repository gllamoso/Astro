package dev.gtcl.reddit.ui.fragments.subreddits.trending

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.NetworkStateItemViewHolder
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.ui.viewholders.TrendingSubredditViewHolder
import kotlin.collections.ArrayList

class TrendingAdapter(private val subredditActions: SubredditActions, private val retry: () -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = ArrayList<TrendingSubredditPost>()
//    private var currentIds: HashSet<String> = HashSet()
//    private var subscribedSubs: HashSet<String> = HashSet()
//    private var favoriteSubs: HashSet<String> = HashSet()
//    private var lastItemReached = false

    var itemClickListener: ItemClickListener? = null

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

//    fun setSubscribedSubs(subs: List<Subreddit>) {
//        subscribedSubs = subs.map { it.displayName.toLowerCase(Locale.ROOT) }.toHashSet()
//        favoriteSubs = subs.filter { it.isFavorite }.map { it.displayName.toLowerCase(Locale.ROOT)}.toHashSet()
//        for(item: TrendingSubredditPost in items){
//            item.setSubscribedTo(subscribedSubs)
//            item.setFavorites(favoriteSubs)
//        }
//        notifyDataSetChanged()
//    }
//
//    fun loadInitial(items: List<TrendingSubredditPost>){
//        this.items.clear()
//        for(post: TrendingSubredditPost in items){
//            post.setSubscribedTo(subscribedSubs)
//            post.setFavorites(favoriteSubs)
//        }
//        this.items.addAll(items)
//        currentIds = items.map { it.post.name }.toHashSet()
//        notifyDataSetChanged()
//    }
//
//    fun loadMore(items: List<TrendingSubredditPost>){
//        val insertionPoint = this.items.size
//        var itemSize = 0
//        for(item: TrendingSubredditPost in items){
//            if(!currentIds.contains(item.post.name)){
//                item.setSubscribedTo(subscribedSubs)
//                item.setFavorites(favoriteSubs)
//                this.items.add(item)
//                currentIds.add(item.post.name)
//                itemSize++
//            }
//        }
//        if(itemSize == 0) {
//            onLastItemReached()
//            lastItemReached = true
//            notifyItemRemoved(this.items.size)
//            return
//        }
//        notifyItemRangeInserted(insertionPoint, itemSize)
//    }

    private val isLoading: Boolean
        get() = networkState != NetworkState.LOADED

    override fun getItemCount(): Int = items.size + if(isLoading) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if(position >= items.size) R.layout.item_network_state
        else R.layout.item_trending_subreddit
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            R.layout.item_trending_subreddit -> (holder as TrendingSubredditViewHolder).bind(items[position], subredditActions) { itemClickListener?.itemClicked(it) }
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
