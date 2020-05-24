package dev.gtcl.reddit.models.reddit

import android.os.Parcelable
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.database.DbMultiReddit
import kotlinx.android.parcel.Parcelize

sealed class ListingType: Parcelable
@Parcelize
object FrontPage : ListingType()
@Parcelize
object All: ListingType()
@Parcelize
object Popular: ListingType()
@Parcelize
class MultiRedditListing(val multiReddit: DbMultiReddit): ListingType()
@Parcelize
class SubredditListing(val sub: Subreddit): ListingType()
@Parcelize
class ProfileListing(val info: ProfileInfo): ListingType()