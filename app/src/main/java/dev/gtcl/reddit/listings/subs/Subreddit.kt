package dev.gtcl.reddit.listings.subs

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Subreddit(
    val name: String = "Undefined",
    @Json(name = "display_name")
    val displayName: String,
    @Json(name = "icon_img")
    val iconImg: String? = "Undefined",
    @Json(name = "display_name_prefixed")
    val displayNamePrefixed: String = "/r/$displayName",
    @Json(name = "title")
    val title: String = "Undefined"
    ) : Parcelable


class SubredditListingResponse(val data: SubredditListingData)

class SubredditListingData(
    val children: List<SubredditChildrenResponse>,
    val after: String?,
    val before: String?
)

data class SubredditChildrenResponse(val data: Subreddit)

data class SubredditNamesResponse(val names: List<String>)