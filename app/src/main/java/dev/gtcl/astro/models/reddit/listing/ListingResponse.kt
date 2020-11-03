package dev.gtcl.astro.models.reddit.listing

class ListingResponse(val data: ListingData)

class ListingData(
    val children: List<ListingChild>,
    val after: String?
)

sealed class ListingChild {
    abstract val data: Item
}

data class CommentChild(override val data: Comment) : ListingChild()
data class AccountChild(override val data: Account) : ListingChild()
data class PostChild(override val data: Post) : ListingChild()
data class MessageChild(override val data: Message) : ListingChild()
data class SubredditChild(override val data: Subreddit) : ListingChild()
data class MoreChild(override val data: More) : ListingChild()
data class MultiRedditChild(override val data: MultiReddit) : ListingChild()
data class TrophyChild(override val data: Trophy) : ListingChild()