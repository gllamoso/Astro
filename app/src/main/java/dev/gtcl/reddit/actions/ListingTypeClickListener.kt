package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.listing.Listing

interface ListingTypeClickListener{
    fun listingTypeClicked(listing: Listing)
}