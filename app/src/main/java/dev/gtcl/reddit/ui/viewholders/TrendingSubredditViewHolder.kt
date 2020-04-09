package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemTrendingSubredditBinding
import dev.gtcl.reddit.listings.Subreddit
import dev.gtcl.reddit.listings.SubredditListing
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.ListingOnClickListeners
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.trending.TrendingSubredditPost

class TrendingSubredditViewHolder private constructor(private val binding: ItemTrendingSubredditBinding, private val onListingClickListener: ListingOnClickListeners): RecyclerView.ViewHolder(binding.root) {
    fun bind(trendingSubredditPost: TrendingSubredditPost) {
        binding.trendingSubredditPost = trendingSubredditPost
        binding.sub1.setOnClickListener{ onListingClickListener.onClick(
            SubredditListing(Subreddit( "",  trendingSubredditPost.titles[0], null, "")))  // TODO: Check if favorite
        }
        binding.sub2.setOnClickListener{ onListingClickListener.onClick(
            SubredditListing(Subreddit( "", "", trendingSubredditPost.titles[1], ""))
        )
        }
        binding.sub3.setOnClickListener{ onListingClickListener.onClick(
            SubredditListing(Subreddit( "", "", trendingSubredditPost.titles[2], ""))
        )
        }
        binding.sub4.setOnClickListener{ onListingClickListener.onClick(
            SubredditListing(Subreddit( "", "", trendingSubredditPost.titles[3], ""))
        )
        }
        binding.sub5.setOnClickListener{ onListingClickListener.onClick(
            SubredditListing(Subreddit( "", "", trendingSubredditPost.titles[4], ""))
        )
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup, onListingClickListener: ListingOnClickListeners): TrendingSubredditViewHolder {
            return TrendingSubredditViewHolder(
                ItemTrendingSubredditBinding.inflate(
                    LayoutInflater.from(parent.context)), onListingClickListener)
        }
    }
}