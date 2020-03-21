package dev.gtcl.reddit.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.gtcl.reddit.users.User
import dev.gtcl.reddit.users.UserSubreddit

@Entity(tableName = "user_table")
data class DatabaseUser constructor(
    @PrimaryKey
    val name: String,
    val iconImg: String?,
    val bannerImg: String?,
    val linkKarma: Int,
    val refreshToken: String?)

fun List<DatabaseUser>.asDomainModel() = map {it.asDomainModel()}

fun DatabaseUser.asDomainModel() = User(
    name = this.name,
    iconImg = this.iconImg,
    linkKarma = this.linkKarma,
    subreddit = UserSubreddit(bannerImg),
    refreshToken = this.refreshToken
)