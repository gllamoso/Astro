package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.ListingType

interface ListingActions{
    fun onClick(listing: ListingType)
}