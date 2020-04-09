package dev.gtcl.reddit.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.gtcl.reddit.listings.Account
import dev.gtcl.reddit.listings.AccountSubreddit
import dev.gtcl.reddit.listings.Subreddit

//     ____                _   ___ _
//    |  _ \ ___  __ _  __| | |_ _| |_ ___ _ __ ___
//    | |_) / _ \/ _` |/ _` |  | || || __/ _ \ '_ ` _ \
//    |  _ <  __/ (_| | (_| |  | || ||  __/ | | | | |
//    |_| \_\___|\__,_|\__,_| |___|\__\___|_| |_| |_|

@Entity(tableName = "read_listing")
data class ItemsRead(
    @PrimaryKey
    val name: String)

//        _                             _
//       / \   ___ ___ ___  _   _ _ __ | |_ ___
//      / _ \ / __/ __/ _ \| | | | '_ \| __/ __|
//     / ___ \ (_| (_| (_) | |_| | | | | |_\__ \
//    /_/   \_\___\___\___/ \__,_|_| |_|\__|___/

@Entity(tableName = "user_table")
data class DbAccount(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconImg: String?,
    val bannerImg: String?,
    val refreshToken: String?) {

    fun asDomainModel() = Account(
        id = this.id,
        name = this.name,
        iconImg = this.iconImg,
        linkKarma = 0,
        commentKarma = 0,
        created = 0L,
        subreddit = AccountSubreddit(bannerImg),
        refreshToken = this.refreshToken
    )
}

fun List<DbAccount>.asAccountDomainModel() = map {it.asDomainModel()}

//      ____        _                  _     _ _ _
//     / ___| _   _| |__  _ __ ___  __| | __| (_) |_ ___
//    \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __/ __|
//     ___) | |_| | |_) | | |  __/ (_| | (_| | | |_\__ \
//    |____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__|___/

@Entity(tableName = "subs")
data class DbSubreddit(
    @PrimaryKey
    val name: String, // Subreddit ID + UserName
    val userId: String, // User subscribed to subreddit
    val displayName: String,
    val iconImg: String?,
    val isFavorite: Boolean){

    fun asDomainModel() = Subreddit(
        name.replace("__$userId", ""),
        displayName,
        iconImg,
        "",
        isFavorite
    )
}

fun List<DbSubreddit>.asSubredditDomainModel() = map { it.asDomainModel() }

