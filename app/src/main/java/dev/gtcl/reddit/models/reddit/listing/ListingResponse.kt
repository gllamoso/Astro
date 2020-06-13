package dev.gtcl.reddit.models.reddit.listing

import com.squareup.moshi.Json
import dev.gtcl.reddit.models.reddit.*

class ListingResponse(val data: ListingData)

class ListingData(
    val children: List<ListingChild>,
    val after: String?
)

sealed class ListingChild(@Json(name="kind") val kind: ItemType){
    abstract val data: Item
}

data class CommentChild(override val data: Comment): ListingChild(
    ItemType.Comment // t1
)
data class AccountChild(override val data: Account): ListingChild(
    ItemType.Account // t2
)
data class PostChild(override val data: Post) : ListingChild(
    ItemType.Post // t3
)
data class MessageChild(override val data: Message): ListingChild(
    ItemType.Message // t4
)
data class SubredditChild(override val data: Subreddit): ListingChild(
    ItemType.Subreddit
)
data class MoreChild(override val data: More): ListingChild(
    ItemType.More // more
)
data class MultiRedditChild(override val data: MultiReddit): ListingChild(
    ItemType.MultiReddit
)
data class TrophyChild(override val data: Award): ListingChild(
    ItemType.Award
)