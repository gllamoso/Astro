package dev.gtcl.reddit.ui.fragments.subreddits.search

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.viewholders.AccountVH
import dev.gtcl.reddit.ui.viewholders.SubredditVH

class SearchAdapter(private val subredditActions: SubredditActions, private val itemClickListener: ItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private var items: MutableList<Item> = ArrayList()

    var networkState: NetworkState = NetworkState.LOADING

    fun submitList(items: List<Item>){
        val previousSize = this.items.size
        this.items.clear()
        notifyItemRangeRemoved(0, previousSize)
        this.items = items.toMutableList()
        notifyItemRangeInserted(0, items.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_account -> AccountVH.create(parent)
            R.layout.item_subreddit -> SubredditVH.create(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when(items[position]){
            is Subreddit -> R.layout.item_subreddit
            is Account -> R.layout.item_account
            else -> throw IllegalArgumentException("Invalid item: ${items[position]}")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(getItemViewType(position)){
            R.layout.item_account -> (holder as AccountVH).bind(items[position] as Account, subredditActions, itemClickListener)
            R.layout.item_subreddit -> (holder as SubredditVH).bind(items[position] as Subreddit, subredditActions, itemClickListener)
        }
    }
}