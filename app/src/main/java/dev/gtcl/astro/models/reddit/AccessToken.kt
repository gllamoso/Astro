package dev.gtcl.astro.models.reddit

import com.squareup.moshi.Json

data class AccessToken(
    @Json(name = "access_token")
    val value: String,
    @Json(name = "token_type")
    val tokenType: String,
    @Json(name = "expires_in")
    val expiresIn: Int,
    @Json(name = "refresh_token")
    var refreshToken: String?
) {
    val authorizationHeader = "bearer $value"
}