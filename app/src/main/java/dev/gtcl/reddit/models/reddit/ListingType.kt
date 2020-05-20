package dev.gtcl.reddit.models.reddit

import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.database.DbMultiReddit

sealed class ListingType
object FrontPage : ListingType()
object All: ListingType()
object Popular: ListingType()
class MultiRedditListing(val multiReddit: DbMultiReddit): ListingType()
class SubredditListing(val sub: Subreddit): ListingType()
class ProfileListing(val info: ProfileInfo): ListingType()