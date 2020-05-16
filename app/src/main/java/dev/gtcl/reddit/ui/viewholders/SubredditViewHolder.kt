package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemSubredditBinding
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.actions.SubsAdapterActions

class SubredditViewHolder private constructor(private val binding: ItemSubredditBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(subreddit: Subreddit, subredditActions: SubredditActions, subsAdapterActions: SubsAdapterActions?, subClicked: () -> Unit){
        binding.sub = subreddit
        binding.root.setOnClickListener {
            subClicked()
        }
        binding.addIcon.setOnClickListener{
            subreddit.userSubscribed = subreddit.userSubscribed != true
            if(!subreddit.userSubscribed!!) {
                subsAdapterActions?.remove(subreddit)
                subreddit.isFavorite = false
            }
            subredditActions.subscribe(subreddit, subreddit.userSubscribed!!)
            binding.invalidateAll()
        }
        binding.favoriteIcon.setOnClickListener {
            subredditActions.addToFavorites(subreddit, !subreddit.isFavorite)
            subreddit.isFavorite = !subreddit.isFavorite
            if(subreddit.isFavorite) {
                subreddit.userSubscribed = true
                subsAdapterActions?.addToFavorites(subreddit)
            } else {
                subsAdapterActions?.removeFromFavorites(subreddit)
            }
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