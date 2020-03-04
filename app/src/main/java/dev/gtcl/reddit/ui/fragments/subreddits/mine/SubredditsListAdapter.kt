package dev.gtcl.reddit.ui.fragments.subreddits.mine

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemSubredditBinding
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.ui.fragments.subreddits.SubredditOnClickListener

class SubredditsListAdapter(private val subredditOnClickListener: SubredditOnClickListener): ListAdapter<Subreddit, SubredditsListAdapter.SubredditViewHolder>(
    DiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubredditViewHolder {
        return SubredditViewHolder(
            ItemSubredditBinding.inflate(LayoutInflater.from(parent.context))
        )
    }

    override fun onBindViewHolder(holder: SubredditViewHolder, position: Int) {
        val subreddit = getItem(position)
        holder.itemView.setOnClickListener { subredditOnClickListener.onClick(subreddit)}
        holder.bind(subreddit)
    }

    fun clear(){

    }

    class SubredditViewHolder(private var binding: ItemSubredditBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(subreddit: Subreddit?){
            binding.sub = subreddit
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Subreddit>() {
        override fun areItemsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem == newItem
        }
    }
}