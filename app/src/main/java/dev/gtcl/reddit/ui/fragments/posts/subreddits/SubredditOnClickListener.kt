package dev.gtcl.reddit.ui.fragments.posts.subreddits

import dev.gtcl.reddit.listings.ListingType

interface SubredditOnClickListener{
    fun onClick(listing: ListingType)
}