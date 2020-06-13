package dev.gtcl.reddit.models.reddit

import dev.gtcl.reddit.models.reddit.listing.ListingChild

data class MoreCommentsResponse(val json: MoreCommentsJson)
data class MoreCommentsJson(val data: MoreCommentsData)
data class MoreCommentsData(val things: List<ListingChild>)