package dev.gtcl.reddit.ui.fragments.subreddits.search

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.gtcl.reddit.posts.SubredditListing
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.ui.fragments.subreddits.SubredditOnClickListener
import dev.gtcl.reddit.ui.fragments.subreddits.mine.MultiAndSubsListAdapter

class SubredditsListAdapter(private val subredditOnClickListener: SubredditOnClickListener): ListAdapter<Subreddit, MultiAndSubsListAdapter.ListingViewHolder>(
    DiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiAndSubsListAdapter.ListingViewHolder {
        return MultiAndSubsListAdapter.ListingViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: MultiAndSubsListAdapter.ListingViewHolder, position: Int) {
        val subreddit = getItem(position)
        holder.itemView.setOnClickListener { subredditOnClickListener.onClick(SubredditListing(subreddit))}
        holder.bind(SubredditListing(subreddit), subredditOnClickListener)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Subreddit>() { // TODO: Remove?
        override fun areItemsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem == newItem
        }
    }
}