package dev.gtcl.reddit.users

import com.squareup.moshi.Json
import dev.gtcl.reddit.database.DatabaseUser

data class User(
    val name: String,
    @Json(name = "icon_img") val iconImg: String?,
    @Json(name = "link_karma") val linkKarma: Int,
    val subreddit: UserSubreddit? =  null,
    // Additional field
    var refreshToken: String?) {

    fun getValidProfileImg(): String {
        val imgRegex = "http.+\\.(png|jpg|gif)".toRegex()
        return imgRegex.find(iconImg!!)!!.value
    }

    fun getValidBannerImg(): String {
        val imgRegex = "http.+\\.(png|jpg|gif)".toRegex()
        return imgRegex.find(subreddit?.bannerImg ?: "")?.value ?: ""
    }
}

data class UserSubreddit(
    @Json(name = "banner_img") val bannerImg: String?
)


fun User.asDatabaseModel() = DatabaseUser(
    name = this.name,
    iconImg = this.iconImg,
    linkKarma = this.linkKarma,
    refreshToken = this.refreshToken
)