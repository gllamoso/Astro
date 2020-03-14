package dev.gtcl.reddit.ui.fragments.subreddits.popular

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import dev.gtcl.reddit.listings.SubredditListing
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.ui.fragments.subreddits.SubredditOnClickListener
import dev.gtcl.reddit.ui.fragments.subreddits.mine.MultiAndSubsListAdapter

class SubredditsPageListAdapter(private val subredditOnClickListener: SubredditOnClickListener): PagedListAdapter<Subreddit, MultiAndSubsListAdapter.ListingViewHolder>(
    SUBREDDIT_COMPARATOR
){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiAndSubsListAdapter.ListingViewHolder {
        return MultiAndSubsListAdapter.ListingViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: MultiAndSubsListAdapter.ListingViewHolder, position: Int) {
        val subreddit = getItem(position)
        holder.itemView.setOnClickListener { subredditOnClickListener.onClick(SubredditListing(subreddit!!))}
        holder.bind(SubredditListing(subreddit!!), subredditOnClickListener)
    }

    companion object {
        val SUBREDDIT_COMPARATOR = object: DiffUtil.ItemCallback<Subreddit>() {
            override fun areItemsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean =
                oldItem.displayName == newItem.displayName

            override fun areContentsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean =
                oldItem == newItem
        }
    }
}