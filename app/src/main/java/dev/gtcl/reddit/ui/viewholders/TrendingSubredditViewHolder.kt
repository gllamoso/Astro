package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemTrendingSubredditBinding
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.trending.TrendingSubredditPost

class TrendingSubredditViewHolder private constructor(private val binding: ItemTrendingSubredditBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(trendingSubredditPost: TrendingSubredditPost, subredditActions: SubredditActions) {
        binding.trendingPost = trendingSubredditPost

        for(i in 0 until 5){
            val binding = when(i){
                0 -> binding.subLayout0
                1 -> binding.subLayout1
                2 -> binding.subLayout2
                3 -> binding.subLayout3
                4 -> binding.subLayout4
                else -> throw IllegalArgumentException("Cannot exceed more than 5 subreddits")
            }

            binding.apply {
                root.setOnClickListener {
                    subredditActions.onClick(trendingSubredditPost.subs[i])
                }
                addIcon.setOnClickListener {
                    trendingSubredditPost.subs[i].apply {
                        isAdded = !isAdded
                        if(!isAdded) isFavorite = false
                        subredditActions.subscribe(this, isAdded, true)
                    }
                    this.invalidateAll()
                }
                favoriteIcon.setOnClickListener {
                    trendingSubredditPost.subs[i].apply{
                        isFavorite = !isFavorite
                        if(isFavorite) this.isAdded = true
                        subredditActions.addToFavorites(this, isFavorite, true)
                    }
                    this.invalidateAll()
                }
            }
        }



        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): TrendingSubredditViewHolder {
            return TrendingSubredditViewHolder(
                ItemTrendingSubredditBinding.inflate(
                    LayoutInflater.from(parent.context)))
        }
    }
}