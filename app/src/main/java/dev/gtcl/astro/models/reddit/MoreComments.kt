package dev.gtcl.astro.models.reddit

import dev.gtcl.astro.models.reddit.listing.ListingChild

data class MoreChildrenResponse(val json: MoreChildrenJson)
data class MoreChildrenJson(val data: MoreChildrenData)
data class MoreChildrenData(val things: List<ListingChild>)