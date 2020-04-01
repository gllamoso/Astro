package dev.gtcl.reddit.ui.fragments.home.subreddits.trending

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemTrendingSubredditBinding
import dev.gtcl.reddit.listings.ListingItem
import dev.gtcl.reddit.listings.Post
import dev.gtcl.reddit.listings.SubredditListing
import dev.gtcl.reddit.listings.subs.Subreddit
import dev.gtcl.reddit.listings.TrendingSubredditPost
import dev.gtcl.reddit.ui.fragments.home.subreddits.SubredditOnClickListener

class TrendingAdapter(private val onClickListener: SubredditOnClickListener) : PagedListAdapter<ListingItem, TrendingAdapter.TrendingSubredditViewHolder>(LISTING_COMPARATOR) {
    override fun onBindViewHolder(holder: TrendingSubredditViewHolder, position: Int) {
        holder.bind(getItem(position) as Post)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingSubredditViewHolder {
        return TrendingSubredditViewHolder.create(parent, onClickListener)
    }

    class TrendingSubredditViewHolder private constructor(private val binding: ItemTrendingSubredditBinding, private val onSubredditClickListener: SubredditOnClickListener): RecyclerView.ViewHolder(binding.root) {
        fun bind(redditPost: Post?) {
            val trendingSubredditPost =
                TrendingSubredditPost(redditPost)
            binding.trendingSubredditPost = trendingSubredditPost
            binding.sub1.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit(displayName = trendingSubredditPost.titles[0]))
            ) }
            binding.sub2.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit(displayName = trendingSubredditPost.titles[1]))
            ) }
            binding.sub3.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit(displayName = trendingSubredditPost.titles[2]))
            ) }
            binding.sub4.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit(displayName = trendingSubredditPost.titles[3]))
            ) }
            binding.sub5.setOnClickListener{ onSubredditClickListener.onClick(
                SubredditListing(Subreddit(displayName = trendingSubredditPost.titles[4]))
            ) }
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup, onSubredditClickListener: SubredditOnClickListener): TrendingSubredditViewHolder {
                return TrendingSubredditViewHolder(ItemTrendingSubredditBinding.inflate(LayoutInflater.from(parent.context)), onSubredditClickListener)
            }
        }
    }

    companion object {
        val POST_COMPARATOR = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.title == newItem.title
            }

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.title == newItem.title
            }

        }

        val LISTING_COMPARATOR = object : DiffUtil.ItemCallback<ListingItem>() {
            override fun areItemsTheSame(oldItem: ListingItem, newItem: ListingItem): Boolean  =
                (oldItem as Post).name == (newItem as Post).name

            override fun areContentsTheSame(oldItem: ListingItem, newItem: ListingItem): Boolean =
                (oldItem as Post).title == (newItem as Post).title

        }
    }
}
