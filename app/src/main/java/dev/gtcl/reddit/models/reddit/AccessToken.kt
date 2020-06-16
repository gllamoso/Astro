package dev.gtcl.reddit.models.reddit

import com.squareup.moshi.Json

data class AccessToken(
    @Json(name="access_token")
    val value: String,
    @Json(name="token_type")
    val tokenType: String,
    @Json(name="expires_in")
    val expiresIn: Int,
    @Json(name="refresh_token")
    var refreshToken: String?){

    private val timeStamp = System.currentTimeMillis()

    val authorizationHeader = "bearer $value"
}