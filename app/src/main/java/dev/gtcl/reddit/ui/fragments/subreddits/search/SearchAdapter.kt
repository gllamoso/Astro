package dev.gtcl.reddit.ui.fragments.subreddits.search

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.SubredditVH

class SearchAdapter(private val subredditActions: SubredditActions, private val itemClickListener: ItemClickListener) : RecyclerView.Adapter<SubredditVH>(){
    private var subs: List<Subreddit> = listOf()

    var networkState: NetworkState = NetworkState.LOADING

    fun submitList(subs: List<Subreddit>){
        this.subs = subs
        notifyDataSetChanged()
    }

    fun updateSubscribedItems(ids: HashSet<String>){
        for(sub: Subreddit in subs){
            sub.userSubscribed = ids.contains(sub.displayName)
        }
        notifyDataSetChanged()
    }

    fun updateFavoriteItems(favoriteSet: HashSet<String>){
        for(sub: Subreddit in subs){
            sub.isFavorite = favoriteSet.contains(sub.displayName)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubredditVH {
        return SubredditVH.create(parent)
    }

    override fun getItemCount() = subs.size

    override fun onBindViewHolder(holder: SubredditVH, position: Int) {
        holder.bind(subs[position], subredditActions, null, false, itemClickListener)
    }

}