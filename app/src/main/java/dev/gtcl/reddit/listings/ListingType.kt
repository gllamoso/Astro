package dev.gtcl.reddit.listings

import dev.gtcl.reddit.subs.Subreddit

sealed class ListingType
object FrontPage : ListingType()
object All: ListingType()
object Saved: ListingType()
object Popular: ListingType()
class MultiReddit(
    val user: String,
    val name: String): ListingType()
class SubredditListing(val sub: Subreddit): ListingType()