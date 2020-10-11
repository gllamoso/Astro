package dev.gtcl.astro.actions

import dev.gtcl.astro.models.reddit.listing.PostListing

interface ListingTypeClickListener {
    fun listingTypeClicked(postListing: PostListing)
}