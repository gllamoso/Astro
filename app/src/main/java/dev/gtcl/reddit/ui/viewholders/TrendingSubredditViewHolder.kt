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

        binding.subLayout0.apply {
            root.setOnClickListener {
                subredditActions.onClick(binding.subLayout0.sub!!)
            }
            addIcon.setOnClickListener {
                trendingSubredditPost.subs[0].apply {
                    isAdded = !isAdded
                    subredditActions.subscribe(this, isAdded, true)
                }
                binding.invalidateAll()
            }
            favoriteIcon.setOnClickListener {
                trendingSubredditPost.subs[0].apply{
                    isFavorite = !isFavorite
                    if(isFavorite) this.isAdded = true
                    subredditActions.addToFavorites(this, isFavorite, true)
                }
                binding.invalidateAll()
            }
        }

        binding.subLayout1.apply {
            root.setOnClickListener {
                subredditActions.onClick(binding.subLayout1.sub!!)
            }
            addIcon.setOnClickListener {
                trendingSubredditPost.subs[1].apply {
                    isAdded = !isAdded
                    subredditActions.subscribe(this, isAdded, true)
                }
                binding.invalidateAll()
            }
            favoriteIcon.setOnClickListener {
                trendingSubredditPost.subs[1].apply{
                    isFavorite = !isFavorite
                    if(isFavorite) this.isAdded = true
                    subredditActions.addToFavorites(this, isFavorite, true)
                }
                binding.invalidateAll()
            }
        }

        binding.subLayout2.apply {
            root.setOnClickListener {
                subredditActions.onClick(binding.subLayout1.sub!!)
            }
            addIcon.setOnClickListener {
                trendingSubredditPost.subs[2].apply {
                    isAdded = !isAdded
                    subredditActions.subscribe(this, isAdded, true)
                }
                binding.invalidateAll()
            }
            favoriteIcon.setOnClickListener {
                trendingSubredditPost.subs[2].apply{
                    isFavorite = !isFavorite
                    if(isFavorite) this.isAdded = true
                    subredditActions.addToFavorites(this, isFavorite, true)
                }
                binding.invalidateAll()
            }
        }

        binding.subLayout3.apply {
            root.setOnClickListener {
                subredditActions.onClick(binding.subLayout1.sub!!)
            }
            addIcon.setOnClickListener {
                trendingSubredditPost.subs[3].apply {
                    isAdded = !isAdded
                    subredditActions.subscribe(this, isAdded, true)
                }
                binding.invalidateAll()
            }
            favoriteIcon.setOnClickListener {
                trendingSubredditPost.subs[3].apply{
                    isFavorite = !isFavorite
                    if(isFavorite) this.isAdded = true
                    subredditActions.addToFavorites(this, isFavorite, true)
                }
                binding.invalidateAll()
            }
        }

        binding.subLayout4.apply {
            root.setOnClickListener {
                subredditActions.onClick(binding.subLayout1.sub!!)
            }
            addIcon.setOnClickListener {
                trendingSubredditPost.subs[4].apply {
                    isAdded = !isAdded
                    subredditActions.subscribe(this, isAdded, true)
                }
                binding.invalidateAll()
            }
            favoriteIcon.setOnClickListener {
                trendingSubredditPost.subs[4].apply{
                    isFavorite = !isFavorite
                    if(isFavorite) this.isAdded = true
                    subredditActions.addToFavorites(this, isFavorite, true)
                }
                binding.invalidateAll()
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