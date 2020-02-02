package dev.gtcl.reddit.users

import android.util.Log
import com.squareup.moshi.Json

data class AccessToken(
    @Json(name="access_token")
    val value: String,
    @Json(name="token_type")
    val tokenType: String,
    @Json(name="expires_in")
    val expiresIn: Int,
    @Json(name="refresh_token")
    val refreshToken: String?){

    val timeStamp = System.currentTimeMillis()

    fun isExpired() = (System.currentTimeMillis() - timeStamp)/1000 > expiresIn
}