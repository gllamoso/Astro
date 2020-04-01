package dev.gtcl.reddit.listings

import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.listings.subs.Subreddit

sealed class ListingType
object FrontPage : ListingType()
object All: ListingType()
object Popular: ListingType()
class MultiReddit(
    val user: String,
    val name: String): ListingType()
class SubredditListing(val sub: Subreddit): ListingType()
class ProfileListing(val info: ProfileInfo): ListingType()