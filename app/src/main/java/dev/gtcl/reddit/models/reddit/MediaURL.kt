package dev.gtcl.reddit.models.reddit

import android.os.Parcelable
import dev.gtcl.reddit.MediaType
import dev.gtcl.reddit.getImgurHashFromUrl
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaURL(
    val url: String,
    val mediaType: MediaType,
    val backupUrl: String? = null,
    var thumbnail: String? = null
): Parcelable {
    @IgnoredOnParcel
    val imgurHash = url.getImgurHashFromUrl()
}