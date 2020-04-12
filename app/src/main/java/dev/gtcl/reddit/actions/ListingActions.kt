package dev.gtcl.reddit.actions

import dev.gtcl.reddit.listings.ListingType

interface ListingActions{
    fun onClick(listing: ListingType)
}