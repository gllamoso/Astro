package dev.gtcl.reddit.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.gtcl.reddit.listings.Account
import dev.gtcl.reddit.listings.AccountSubreddit

@Entity(tableName = "user_table")
data class DbAccount constructor(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconImg: String?,
    val bannerImg: String?,
    val refreshToken: String?)

fun List<DbAccount>.asDomainModel() = map {it.asDomainModel()}

fun DbAccount.asDomainModel() = Account(
    id = this.id,
    name = this.name,
    iconImg = this.iconImg,
    linkKarma = 0,
    commentKarma = 0,
    created = 0L,
    subreddit = AccountSubreddit(bannerImg),
    refreshToken = this.refreshToken
)