package dev.gtcl.astro.models.reddit.listing

import android.os.Parcelable
import dev.gtcl.astro.ProfileInfo
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

sealed class PostListing : Parcelable, Serializable

@Parcelize
object FrontPage : PostListing()

@Parcelize
object All : PostListing()

@Parcelize
object Popular : PostListing()

@Parcelize
class SearchListing(val query: String) : PostListing()

@Parcelize
class MultiRedditListing(val name: String, val path: String) : PostListing()

@Parcelize
class SubredditListing(val displayName: String) : PostListing()

@Parcelize
class ProfileListing(val user: String?, val info: ProfileInfo) : PostListing()