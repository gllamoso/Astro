package dev.gtcl.astro.models.reddit

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AccessToken(
    @Json(name = "access_token")
    val value: String,
    @Json(name = "token_type")
    val tokenType: String,
    @Json(name = "expires_in")
    val expiresIn: Int,
    @Json(name = "refresh_token")
    var refreshToken: String?
) : Parcelable {
    @IgnoredOnParcel
    val authorizationHeader = "bearer $value"
}