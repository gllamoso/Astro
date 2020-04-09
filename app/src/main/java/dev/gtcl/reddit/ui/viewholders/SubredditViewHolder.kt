package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemSubredditBinding
import dev.gtcl.reddit.listings.Subreddit
import dev.gtcl.reddit.listings.SubredditListing
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.ListingOnClickListeners

class SubredditViewHolder(private val binding: ItemSubredditBinding): RecyclerView.ViewHolder(binding.root) {
    private var isAdded = false
    fun bind(subreddit: Subreddit, listingOnClickListeners: ListingOnClickListeners, added: Boolean){
        binding.sub = subreddit
        isAdded = added
        binding.added = isAdded
        binding.root.setOnClickListener {
            listingOnClickListeners.onClick(SubredditListing(subreddit))
        }
        binding.addIcon.setOnClickListener{
            isAdded = !isAdded
            binding.added = isAdded
            binding.invalidateAll()
        }
        binding.favoriteIcon.setOnClickListener {
            listingOnClickListeners.addToFavorites(SubredditListing(subreddit), !subreddit.isFavorite)
            subreddit.isFavorite = !subreddit.isFavorite
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