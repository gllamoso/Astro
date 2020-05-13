package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemSubredditBinding
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.actions.SubsAdapterActions

class SubredditViewHolder private constructor(private val binding: ItemSubredditBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(subreddit: Subreddit, subredditActions: SubredditActions, subsAdapterActions: SubsAdapterActions?){
        binding.sub = subreddit
        binding.root.setOnClickListener {
            subredditActions.onClick(subreddit)
        }
        binding.addIcon.setOnClickListener{
            subreddit.isAddedToDb = !subreddit.isAddedToDb
            if(!subreddit.isAddedToDb) {
                subsAdapterActions?.remove(subreddit)
            }
            subredditActions.subscribe(subreddit, subreddit.isAddedToDb, subsAdapterActions == null)
            binding.invalidateAll()
        }
        binding.favoriteIcon.setOnClickListener {
            subredditActions.addToFavorites(subreddit, !subreddit.isFavorite, subsAdapterActions == null)
            subreddit.isFavorite = !subreddit.isFavorite
            if(subreddit.isFavorite)
                subsAdapterActions?.addToFavorites(subreddit)
            else
                subsAdapterActions?.removeFromFavorites(subreddit)
            binding.invalidateAll()
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): SubredditViewHolder {
            return SubredditViewHolder(ItemSubredditBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}