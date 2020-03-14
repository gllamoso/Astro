package dev.gtcl.reddit.ui.fragments.subreddits

import dev.gtcl.reddit.listings.ListingType

interface SubredditOnClickListener{
    fun onClick(listing: ListingType)
}