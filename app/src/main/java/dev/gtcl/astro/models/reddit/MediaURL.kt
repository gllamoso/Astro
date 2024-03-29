package dev.gtcl.astro.models.reddit

import android.os.Parcelable
import dev.gtcl.astro.url.MediaType
import dev.gtcl.astro.url.getImgurHashFromUrl
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaURL(
    val url: String,
    val mediaType: MediaType,
    val backupUrl: String? = null,
    val thumbnail: String? = null,
) : Parcelable {
    @IgnoredOnParcel
    val imgurHash = url.getImgurHashFromUrl()
}