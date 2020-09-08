package dev.gtcl.astro.models.reddit

import android.os.Parcelable
import dev.gtcl.astro.MediaType
import dev.gtcl.astro.getImgurHashFromUrl
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MediaURL(
    val url: String,
    val mediaType: MediaType,
    val backupUrl: String? = null
) : Parcelable {
    @IgnoredOnParcel
    val imgurHash = url.getImgurHashFromUrl()
}