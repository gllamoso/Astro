package dev.gtcl.reddit.models.reddit

import dev.gtcl.reddit.ProfileInfo

sealed class ListingType
object FrontPage : ListingType()
object All: ListingType()
object Popular: ListingType()
class MultiRedditListing(val multiReddit: MultiReddit): ListingType()
class SubredditListing(val sub: Subreddit): ListingType()
class ProfileListing(val info: ProfileInfo): ListingType()