package dev.gtcl.reddit.models.reddit

import dev.gtcl.reddit.ProfileInfo

sealed class ListingType
object FrontPage : ListingType()
object All: ListingType()
object Popular: ListingType()
class MultiReddit(val user: String, val name: String): ListingType()
class SubredditListing(val sub: Subreddit): ListingType()
class ProfileListing(val info: ProfileInfo): ListingType()