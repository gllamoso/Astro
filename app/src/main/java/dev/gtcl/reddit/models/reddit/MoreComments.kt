package dev.gtcl.reddit.models.reddit

import dev.gtcl.reddit.models.reddit.listing.ListingChild

data class MoreChildrenResponse(val json: MoreChildrenJson)
data class MoreChildrenJson(val data: MoreChildrenData)
data class MoreChildrenData(val things: List<ListingChild>)