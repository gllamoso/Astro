package dev.gtcl.reddit.ui.fragments.posts.subreddit_selector.trending

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemTrendingSubredditBinding
import dev.gtcl.reddit.posts.RedditPost
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.posts.TrendingSubredditPost

class TrendingAdapter(private val onClickListener: OnClickListener) : PagedListAdapter<RedditPost, TrendingAdapter.TrendingSubredditViewHolder>(
    POST_COMPARATOR
) {
    override fun onBindViewHolder(holder: TrendingSubredditViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingSubredditViewHolder {
        return TrendingSubredditViewHolder.create(parent, onClickListener)
    }

    class TrendingSubredditViewHolder private constructor(private val binding: ItemTrendingSubredditBinding, val onSubredditClickListener: OnClickListener): RecyclerView.ViewHolder(binding.root) {
        fun bind(redditPost: RedditPost?) {
            val trendingSubredditPost =
                TrendingSubredditPost(redditPost)
            binding.trendingSubredditPost = trendingSubredditPost
            binding.sub1.setOnClickListener{ onSubredditClickListener.onClick(
                Subreddit(displayName = trendingSubredditPost.titles[0])
            ) }
            binding.sub2.setOnClickListener{ onSubredditClickListener.onClick(
                Subreddit(displayName = trendingSubredditPost.titles[1])
            ) }
            binding.sub3.setOnClickListener{ onSubredditClickListener.onClick(
                Subreddit(displayName = trendingSubredditPost.titles[2])
            ) }
            binding.sub4.setOnClickListener{ onSubredditClickListener.onClick(
                Subreddit(displayName = trendingSubredditPost.titles[3])
            ) }
            binding.sub5.setOnClickListener{ onSubredditClickListener.onClick(
                Subreddit(displayName = trendingSubredditPost.titles[4])
            ) }
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup, onSubredditClickListener: OnClickListener): TrendingSubredditViewHolder {
                return TrendingSubredditViewHolder(ItemTrendingSubredditBinding.inflate(LayoutInflater.from(parent.context)), onSubredditClickListener)
            }
        }
    }

    companion object {
        val POST_COMPARATOR = object : DiffUtil.ItemCallback<RedditPost>() {
            override fun areItemsTheSame(oldItem: RedditPost, newItem: RedditPost): Boolean {
                return oldItem.title == newItem.title
            }

            override fun areContentsTheSame(oldItem: RedditPost, newItem: RedditPost): Boolean {
                return oldItem.title == newItem.title
            }

        }
    }

    class OnClickListener(val clickListener: (subreddit: Subreddit) -> Unit) {
        fun onClick(subreddit: Subreddit) = clickListener(subreddit)
    }
}
