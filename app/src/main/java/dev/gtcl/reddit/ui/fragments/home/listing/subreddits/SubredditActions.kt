package dev.gtcl.reddit.ui.fragments.home.listing.subreddits

import dev.gtcl.reddit.listings.ListingType

interface SubredditActions{
    fun onClick(listing: ListingType)
}