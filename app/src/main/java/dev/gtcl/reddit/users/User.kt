package dev.gtcl.reddit.users

import com.squareup.moshi.Json
import dev.gtcl.reddit.database.DatabaseUser

data class User(
    val name: String,
    @Json(name = "icon_img") val iconImg: String?,
    @Json(name = "link_karma") val linkKarma: Int,
    // Additional field
    var refreshToken: String?)

fun User.asDatabaseModel() = DatabaseUser(
    name = this.name,
    iconImg = this.iconImg,
    linkKarma = this.linkKarma,
    refreshToken = this.refreshToken
)