package dev.gtcl.reddit.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.models.reddit.AccountSubreddit
import dev.gtcl.reddit.models.reddit.MultiReddit
import dev.gtcl.reddit.models.reddit.Subreddit

//   _____                _   _____ _
//  |  __ \              | | |_   _| |
//  | |__) |___  __ _  __| |   | | | |_ ___ _ __ ___
//  |  _  // _ \/ _` |/ _` |   | | | __/ _ \ '_ ` _ \
//  | | \ \  __/ (_| | (_| |  _| |_| ||  __/ | | | | |
//  |_|  \_\___|\__,_|\__,_| |_____|\__\___|_| |_| |_|

@Entity(tableName = "read_listing")
data class ItemRead(
    @PrimaryKey
    val name: String)

//                                     _
//      /\                            | |
//     /  \   ___ ___ ___  _   _ _ __ | |_ ___
//    / /\ \ / __/ __/ _ \| | | | '_ \| __/ __|
//   / ____ \ (_| (_| (_) | |_| | | | | |_\__ \
//  /_/    \_\___\___\___/ \__,_|_| |_|\__|___/
//


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

//     _____       _                  _     _ _ _
//    / ____|     | |                | |   | (_) |
//   | (___  _   _| |__  _ __ ___  __| | __| |_| |_ ___
//    \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __/ __|
//    ____) | |_| | |_) | | |  __/ (_| | (_| | | |_\__ \
//   |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__|___/
//

@Entity(tableName = "subs")
data class DbSubreddit(
    @PrimaryKey
    val id: String, // {Subreddit ID}__{User ID}
    val userId: String, // User subscribed to subreddit
    val displayName: String,
    val title: String,
    val iconImg: String?,
    val isFavorite: Boolean){

    fun asDomainModel() = Subreddit(
        id.replace("__$userId", ""),
        displayName,
        iconImg,
        title,
        "",
        true,
        "",
        isFavorite
    )
}

fun List<DbSubreddit>.asDomainModel() = map { it.asDomainModel() }

//   __  __       _ _   _        _____          _     _ _ _
//  |  \/  |     | | | (_)      |  __ \        | |   | (_) |
//  | \  / |_   _| | |_ _ ______| |__) |___  __| | __| |_| |_ ___
//  | |\/| | | | | | __| |______|  _  // _ \/ _` |/ _` | | __/ __|
//  | |  | | |_| | | |_| |      | | \ \  __/ (_| | (_| | | |_\__ \
//  |_|  |_|\__,_|_|\__|_|      |_|  \_\___|\__,_|\__,_|_|\__|___/
//

@Entity(tableName = "multis")
data class DbMultiReddit(
    @PrimaryKey
    val id: String, // {Multi-Reddit Name}__{User ID}
    val name: String,
    val userId: String,
    val path: String,
    val iconUrl: String
) {
    fun asDomainModel() = MultiReddit(
        true,
        name,
        listOf(),
        path,
        "",
        userId,
        iconUrl
    )
}