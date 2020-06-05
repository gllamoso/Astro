package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemTrendingSubredditBinding
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.ui.fragments.subreddits.trending.TrendingSubredditPost

class TrendingSubredditVH private constructor(private val binding: ItemTrendingSubredditBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        trendingSubredditPost: TrendingSubredditPost,
        subredditActions: SubredditActions,
        subredditClickAction: (Subreddit) -> Unit
    ) {
        binding.trendingPost = trendingSubredditPost

        for (i in 0 until 5) {
            val binding = when (i) {
                0 -> binding.subLayout0
                1 -> binding.subLayout1
                2 -> binding.subLayout2
                3 -> binding.subLayout3
                4 -> binding.subLayout4
                else -> throw IllegalArgumentException("Cannot exceed more than 5 subreddits")
            }

            binding.apply {
                root.setOnClickListener {
                    subredditClickAction(trendingSubredditPost.subs[i])
                }
                addButton.setOnClickListener {
                    trendingSubredditPost.subs[i].apply {
                        userSubscribed = userSubscribed != true
                        subredditActions.subscribe(this, userSubscribed!!)
                    }
                    this.invalidateAll()
                }
            }
        }



        binding.executePendingBindings()
    }

    companion object {
        fun create(parent: ViewGroup): TrendingSubredditVH {
            return TrendingSubredditVH(
                ItemTrendingSubredditBinding.inflate(
                    LayoutInflater.from(parent.context)
                )
            )
        }
    }
}