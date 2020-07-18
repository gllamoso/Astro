package dev.gtcl.reddit.ui.fragments.subreddits.multireddit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemSubredditInMultiredditBinding
import dev.gtcl.reddit.models.reddit.listing.Subreddit

class MultiRedditSubredditsAdapter(private val onSubredditRemovedListener: OnSubredditRemovedListener) : ListAdapter<Subreddit, MultiRedditSubredditsAdapter.MultiRedditSubredditVH>(DiffCallback){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiRedditSubredditVH = MultiRedditSubredditVH.create(parent)

    override fun onBindViewHolder(holder: MultiRedditSubredditVH, position: Int) {
        holder.bind(getItem(position), onSubredditRemovedListener)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Subreddit>(){
        override fun areItemsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Subreddit, newItem: Subreddit): Boolean {
            return oldItem == newItem
        }

    }

    class MultiRedditSubredditVH private constructor(private val binding: ItemSubredditInMultiredditBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(sub: Subreddit, onSubredditRemovedListener: OnSubredditRemovedListener){
            binding.sub = sub
            binding.removeButton.setOnClickListener {
                onSubredditRemovedListener.onRemove(sub, adapterPosition)
            }
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup) =
                MultiRedditSubredditVH(
                    ItemSubredditInMultiredditBinding.inflate(LayoutInflater.from(parent.context))
                )
        }
    }

    interface OnSubredditRemovedListener{
        fun onRemove(subreddit: Subreddit, position: Int)
    }

}