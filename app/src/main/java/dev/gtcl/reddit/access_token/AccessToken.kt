package dev.gtcl.reddit.access_token

import com.squareup.moshi.Json

data class AccessToken(
    @Json(name="access_token")
    val accessToken: String,
    @Json(name="token_type")
    val tokenType: String,
    @Json(name="expires_in")
    val expiresIn: Int,
    @Json(name="refresh_token")
    val refreshToken: String?){

    val timeStamp = System.currentTimeMillis()
}