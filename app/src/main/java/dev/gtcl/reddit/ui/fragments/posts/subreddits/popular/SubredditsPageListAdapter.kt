package dev.gtcl.reddit.ui.fragments.posts.subreddits.popular

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import dev.gtcl.reddit.databinding.ItemSubredditBinding
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.ui.fragments.posts.subreddits.mine.SubredditsListAdapter

class SubredditsPageListAdapter(private val onClickListener: OnClickListener, private val retryCallback: () -> Unit): PagedListAdapter<Subreddit, SubredditsListAdapter.SubredditViewHolder>(
    SUBREDDIT_COMPARATOR
){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubredditsListAdapter.SubredditViewHolder {
        return SubredditsListAdapter.SubredditViewHolder(
            ItemSubredditBinding.inflate(LayoutInflater.from(parent.context))
        )
    }

    override fun onBindViewHolder(holder: SubredditsListAdapter.SubredditViewHolder, position: Int) {
        val subreddit = getItem(position)
        holder.itemView.setOnClickListener { onClickListener.onClick(subreddit) }
        holder.bind(subreddit)
    }

    companion object {
        val SUBREDDIT_COMPARATOR = object: DiffUtil.ItemCallback<Subreddit>() {
            override fun areItemsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean =
                oldItem.displayName == newItem.displayName

            override fun areContentsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean =
                oldItem == newItem
        }
    }

    class OnClickListener(val clickListener: (subreddit: Subreddit?) -> Unit) {
        fun onClick(subreddit: Subreddit?) = clickListener(subreddit)
    }
}