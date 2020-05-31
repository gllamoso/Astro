package dev.gtcl.reddit.models.reddit

import android.os.Parcelable
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.database.Subscription
import kotlinx.android.parcel.Parcelize

sealed class ListingType: Parcelable
@Parcelize
object FrontPage : ListingType()
@Parcelize
object All: ListingType()
@Parcelize
object Popular: ListingType()
@Parcelize
class MultiRedditListing(val multiReddit: MultiReddit): ListingType()
@Parcelize
class SubredditListing(val sub: Subreddit): ListingType()
@Parcelize
class SubscriptionListing(val subscription: Subscription): ListingType()
@Parcelize
class ProfileListing(val info: ProfileInfo): ListingType()