package dev.gtcl.astro.actions

import dev.gtcl.astro.models.reddit.listing.Listing

interface ListingTypeClickListener{
    fun listingTypeClicked(listing: Listing)
}