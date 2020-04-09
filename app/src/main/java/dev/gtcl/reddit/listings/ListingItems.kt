package dev.gtcl.reddit.listings

import android.net.Uri
import android.os.Parcelable
import com.squareup.moshi.Json
import dev.gtcl.reddit.database.DbAccount
import dev.gtcl.reddit.database.DbSubreddit
import dev.gtcl.reddit.database.ItemsRead
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

enum class ItemType {
    @Json(name="t1")
    Comment,
    @Json(name="t2")
    Account,
    @Json(name="t3")
    Post,
    @Json(name="t4")
    Message,
    @Json(name="t5")
    Subreddit,
    @Json(name="t6")
    Award,
    @Json(name="more")
    More
}

sealed class Item(val kind: ItemType){
    abstract val depth: Int
    abstract val id: String?
    abstract val name: String
    var hiddenPoints = 0 // Hide if > 0
}

class ListingResponse(val data: ListingData)

class ListingData(
    val children: List<ListingChild>,
    val after: String?
)

sealed class ListingChild(@Json(name="kind") val kind: ItemType){
    abstract val data: Item
}

data class CommentChild(override val data: Comment): ListingChild(
    ItemType.Comment // t1
)
data class AccountChild(override val data: Account): ListingChild(
    ItemType.Account // t2
)
data class PostChild(override val data: Post) : ListingChild(
    ItemType.Post // t3
)
data class SubredditChild(override val data: Subreddit): ListingChild(
    ItemType.Subreddit
)
data class MoreChild(override val data: More): ListingChild(
    ItemType.More // more
)

// http://patorjk.com/software/taag/#p=display&f=Ivrit&t=t1%20-%20Comment

//     _   _            ____                                     _
//    | |_/ |          / ___|___  _ __ ___  _ __ ___   ___ _ __ | |_
//    | __| |  _____  | |   / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __|
//    | |_| | |_____| | |__| (_) | | | | | | | | | | |  __/ | | | |_
//     \__|_|          \____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__|

data class Comment( // TODO: Add more properties: saved, liked, all_awardings
    override val name: String,
    override val id: String = name.replace("t1_", ""),
    override var depth: Int = 0,
    val author: String,
    @Json(name="author_fullname")
    val authorFullName: String?,
    val body: String,
    val score: Int,
    @Json(name="created_utc")
    val created: Long,
    var isPartiallyCollapsed: Boolean = false
): Item(ItemType.Comment)


//     _   ____               _                             _
//    | |_|___ \             / \   ___ ___ ___  _   _ _ __ | |_
//    | __| __) |  _____    / _ \ / __/ __/ _ \| | | | '_ \| __|
//    | |_ / __/  |_____|  / ___ \ (_| (_| (_) | |_| | | | | |_
//     \__|_____|         /_/   \_\___\___\___/ \__,_|_| |_|\__|

data class Account(
    override val id: String,
    override val name: String,
    @Json(name = "icon_img") val iconImg: String?,
    @Json(name = "link_karma") val linkKarma: Int,
    @Json(name = "comment_karma") val commentKarma: Int,
    @Json(name = "created_utc") val created: Long,
    val subreddit: AccountSubreddit? =  null,
    // Additional field
    var refreshToken: String?
) : Item(ItemType.Account) {

    override val depth: Int = 0

    fun getValidProfileImg(): String {
        val imgRegex = "http.+\\.(png|jpg|gif)".toRegex()
        return imgRegex.find(iconImg!!)!!.value
    }

    fun getValidBannerImg(): String {
        val imgRegex = "http.+\\.(png|jpg|gif)".toRegex()
        return imgRegex.find(subreddit?.bannerImg ?: "")?.value ?: ""
    }

    fun asDbModel() = DbAccount(
        id = this.id,
        name = this.name,
        iconImg = this.iconImg,
        bannerImg = this.subreddit?.bannerImg,
        refreshToken = this.refreshToken
    )
}

data class AccountSubreddit(
    @Json(name = "banner_img") val bannerImg: String?
)

//     _   _____           ____           _
//    | |_|___ /          |  _ \ ___  ___| |_
//    | __| |_ \   _____  | |_) / _ \/ __| __|
//    | |_ ___) | |_____| |  __/ (_) \__ \ |_
//     \__|____/          |_|   \___/|___/\__|

@Parcelize
data class Post(
    override val name: String,
    override val id: String = name.replace("t3_", ""),
    var saved: Boolean,
    val title: String,
    val score: Int,
    val author: String,
    val subreddit: String,
    @Json(name = "num_comments")
    val numComments: Int,
    @Json(name = "created_utc")
    val created: Long,
    val thumbnail: String?,
    val url: String?,
    var likes: Boolean?,
    var hidden: Boolean,
    val permalink: String,
    val selftext: String,
    @Json(name = "is_self")
    val isSelf: Boolean, // if true, post is a text
    @Json(name = "upvote_ratio")
    val upvoteRatio: Double?,
    val secureMedia: SecureMedia?,
    val preview: Preview?,
    val media: Media?
) : Parcelable, Item(ItemType.Post) {

    @IgnoredOnParcel
    override val depth = 0

    fun asReadListing() = ItemsRead(this.name)

    fun isPicture(): Boolean{
        url?.let {
            val uri = Uri.parse(it)
            uri.lastPathSegment?.let { lastPathSegment ->
                return lastPathSegment.contains("(.jpg|.png|.gif|.svg)".toRegex())
            }
        }
        return false
    }

    fun getShortLink(): String = "http://redd.it/$id"
}

// Reddit API Response
@Parcelize
data class Preview(
    @Json(name = "reddit_video_preview")
    val redditVideo: RedditVideo?
) : Parcelable

@Parcelize
data class SecureMedia(
    @Json(name = "reddit_video")
    val redditVideo: RedditVideo?
) : Parcelable

@Parcelize
data class Media(
    @Json(name = "reddit_video")
    val redditVideo: RedditVideo?
): Parcelable

@Parcelize
data class RedditVideo(
    @Json(name = "hls_url")
    val hlsUrl: String
) : Parcelable

//     _   ____            ____        _                  _     _ _ _
//    | |_| ___|          / ___| _   _| |__  _ __ ___  __| | __| (_) |_
//    | __|___ \   _____  \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|
//    | |_ ___) | |_____|  ___) | |_| | |_) | | |  __/ (_| | (_| | | |_
//     \__|____/          |____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__|

data class Subreddit(
    override val name: String,
    @Json(name = "display_name")
    val displayName: String,
    @Json(name = "icon_img")
    val iconImg: String?,
    @Json(name = "title")
    val title: String,
    @Transient
    var isFavorite: Boolean = false
) : Item(ItemType.Subreddit) {
    override val depth: Int = 0
    override val id = name.replace("t5_","")

    fun asDbModel(userId: String) = DbSubreddit(
        "${name}__${userId}",
        userId,
        displayName,
        iconImg,
        isFavorite
    )
}

fun List<Subreddit>.asSubredditDatabaseModels(userId: String) = map { it.asDbModel(userId) }

data class SubredditNamesResponse(val names: List<String>)

//   _    __                _                        _
//  | |_ / /_              / \__      ____ _ _ __ __| |
//  | __| '_ \   _____    / _ \ \ /\ / / _` | '__/ _` |
//  | |_| (_) | |_____|  / ___ \ V  V / (_| | | | (_| |
//   \__|\___/          /_/   \_\_/\_/ \__,_|_|  \__,_|


class TrophyListingResponse(val data: TrophyListingData)
class TrophyListingData(val trophies: List<TrophyListing>)
data class TrophyListing(override val data: Award): ListingChild(
    ItemType.Award
)

@Parcelize
data class Award(
    override val name: String,
    override val id: String?,
    @Json(name = "icon_70") val icon70: String,
    @Json(name = "icon_40") val icon40: String
) : Parcelable, Item(ItemType.Award){
    @IgnoredOnParcel
    override val depth = 0
}

//                                        __  __
//     _ __ ___   ___  _ __ ___          |  \/  | ___  _ __ ___
//    | '_ ` _ \ / _ \| '__/ _ \  _____  | |\/| |/ _ \| '__/ _ \
//    | | | | | | (_) | | |  __/ |_____| | |  | | (_) | | |  __/
//    |_| |_| |_|\___/|_|  \___|         |_|  |_|\___/|_|  \___|

data class More(
    override val name: String,
    override val id: String = name.replace("t1_", ""),
    override var depth: Int,
    val parentId: String,
    val children: List<String>,
    var count: Int
): Item(ItemType.More) {



    fun getChildrenAsValidString(): String {
        if(this.children.isEmpty()) return ""
        val sb = StringBuilder()
        for(item in this.children)
            sb.append("$item,")
        sb.deleteCharAt(sb.length - 1)
        return sb.toString()
    }

    fun isContinueThreadLink() = id == "_"
}
