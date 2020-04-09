package dev.gtcl.reddit.ui.fragments.home.listing.subreddits

import dev.gtcl.reddit.listings.ListingType

interface ListingOnClickListeners{
    fun onClick(listing: ListingType)
    fun addToFavorites(listing: ListingType, favorite: Boolean)
    fun addOrSubscribe(listing: ListingType)
    fun removedOrUnsubscribed(listing: ListingType)
}