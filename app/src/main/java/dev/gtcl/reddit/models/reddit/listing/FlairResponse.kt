package dev.gtcl.reddit.models.reddit.listing

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Flair(
    var text: String,
    @Json(name = "text_editable") val textEditable: Boolean,
    val id: String
): Parcelable