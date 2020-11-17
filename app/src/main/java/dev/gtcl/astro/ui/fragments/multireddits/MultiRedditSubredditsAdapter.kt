package dev.gtcl.astro.ui.fragments.multireddits

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.models.reddit.listing.Subreddit
import dev.gtcl.astro.ui.viewholders.OnSubredditRemovedListener
import dev.gtcl.astro.ui.viewholders.SubredditInMultiRedditVH

class MultiRedditSubredditsAdapter(
    private val itemClickListener: ItemClickListener,
    private val onSubredditRemovedListener: OnSubredditRemovedListener?
) :
    ListAdapter<Subreddit, SubredditInMultiRedditVH>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubredditInMultiRedditVH =
        SubredditInMultiRedditVH.create(parent)

    override fun onBindViewHolder(holder: SubredditInMultiRedditVH, position: Int) {
        holder.bind(getItem(position), itemClickListener, onSubredditRemovedListener)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Subreddit>() {
        override fun areItemsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem == newItem
        }

    }

}