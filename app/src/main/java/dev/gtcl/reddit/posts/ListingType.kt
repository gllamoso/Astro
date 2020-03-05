package dev.gtcl.reddit.posts

import dev.gtcl.reddit.subs.Subreddit

sealed class ListingType
object FrontPage : ListingType()
class SubredditListing(val sub: Subreddit): ListingType()
class MultiReddit(
    val user: String,
    val name: String): ListingType()