package dev.gtcl.reddit.models.reddit.listing

import android.os.Parcelable
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.database.Subscription
import kotlinx.android.parcel.Parcelize

sealed class Listing: Parcelable
@Parcelize
object FrontPage : Listing()
@Parcelize
object All: Listing()
@Parcelize
object Popular: Listing()
@Parcelize
class SearchListing(val query: String): Listing()
@Parcelize
class MultiRedditListing(val multiReddit: MultiReddit): Listing()
@Parcelize
class SubredditListing(val displayName: String): Listing()
@Parcelize
class SubscriptionListing(val subscription: Subscription): Listing()
@Parcelize
class ProfileListing(val info: ProfileInfo): Listing()