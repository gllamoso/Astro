package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.listing.ListingType

interface ListingTypeClickListener{
    fun listingTypeClicked(listing: ListingType)
}