package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.trending

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.ItemTrendingSubredditBinding
import dev.gtcl.reddit.listings.Subreddit
import dev.gtcl.reddit.listings.SubredditListing
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.NetworkStateItemViewHolder
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.SubredditActions

class TrendingAdapter(private val actions: SubredditActions, private val retry: () -> Unit, private val onLastItemReached: () -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = ArrayList<TrendingSubredditPost>()
    private var currentIds: HashSet<String> = HashSet()
    var lastItemReached = false

    private var networkState = NetworkState.LOADED
    fun setNetworkState(networkState: NetworkState){
        this.networkState = networkState
    }

    fun loadInitial(items: List<TrendingSubredditPost>){
        this.items.clear()
        this.items.addAll(items)
        currentIds = items.map { it.post.name }.toHashSet()
        notifyDataSetChanged()
    }

    fun loadMore(items: List<TrendingSubredditPost>){
        val insertionPoint = this.items.size
        var itemSize = 0
        for(item: TrendingSubredditPost in items){
            if(!currentIds.contains(item.post.name)){
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
        notifyItemRangeChanged(insertionPoint, itemSize)
    }

    private fun hasNetworkStateView() = (networkState != NetworkState.LOADED)

    override fun getItemCount(): Int = items.size + if(hasNetworkStateView() && !lastItemReached) 1 else 0

    override fun getItemViewType(position: Int): Int {
        return if(position >= items.size) R.layout.item_network_state
        else R.layout.item_trending_subreddit
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            R.layout.item_trending_subreddit -> (holder as TrendingSubredditViewHolder).bind(items[position])
            R.layout.item_network_state -> (holder as NetworkStateItemViewHolder).bindTo(networkState)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_trending_subreddit -> TrendingSubredditViewHolder.create(parent, actions)
            R.layout.item_network_state -> NetworkStateItemViewHolder.create(parent, retry)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    class TrendingSubredditViewHolder private constructor(private val binding: ItemTrendingSubredditBinding, private val onSubredditClickListener: SubredditActions): RecyclerView.ViewHolder(binding.root) {
        fun bind(trendingSubredditPost: TrendingSubredditPost) {
            binding.trendingSubredditPost = trendingSubredditPost
            binding.sub1.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit( "", "", trendingSubredditPost.titles[0], null, title = null)))
            }
            binding.sub2.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit( "", "", trendingSubredditPost.titles[1], null, title = null)))
            }
            binding.sub3.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit( "", "", trendingSubredditPost.titles[2], null, title = null)))
            }
            binding.sub4.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit( "", "", trendingSubredditPost.titles[3], null, title = null)))
            }
            binding.sub5.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit( "", "", trendingSubredditPost.titles[4], null, title = null)))
            }
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup, onSubredditClickListener: SubredditActions): TrendingSubredditViewHolder {
                return TrendingSubredditViewHolder(ItemTrendingSubredditBinding.inflate(LayoutInflater.from(parent.context)), onSubredditClickListener)
            }
        }
    }

}
