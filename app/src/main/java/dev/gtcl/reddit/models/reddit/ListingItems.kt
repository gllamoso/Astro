package dev.gtcl.reddit.models.reddit

import android.net.Uri
import android.os.Parcelable
import com.squareup.moshi.Json
import dev.gtcl.reddit.SubscriptionType
import dev.gtcl.reddit.database.*
import dev.gtcl.reddit.toValidImgUrl
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
    More,
    @Json(name="LabeledMulti")
    MultiReddit
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
data class MessageChild(override val data: Message): ListingChild(
    ItemType.Message // t4
)
data class SubredditChild(override val data: Subreddit): ListingChild(
    ItemType.Subreddit
)
data class MoreChild(override val data: More): ListingChild(
    ItemType.More // more
)
data class MultiRedditChild(override val data: MultiReddit): ListingChild(
    ItemType.MultiReddit
)

// http://patorjk.com/software/taag/#p=display&f=Big&t=t1%20-%20Comment

//   _  __             _____                                     _
//  | |/_ |           / ____|                                   | |
//  | |_| |  ______  | |     ___  _ __ ___  _ __ ___   ___ _ __ | |_
//  | __| | |______| | |    / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __|
//  | |_| |          | |___| (_) | | | | | | | | | | |  __/ | | | |_
//   \__|_|           \_____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__|

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


//   _   ___                                               _
//  | | |__ \               /\                            | |
//  | |_   ) |  ______     /  \   ___ ___ ___  _   _ _ __ | |_
//  | __| / /  |______|   / /\ \ / __/ __/ _ \| | | | '_ \| __|
//  | |_ / /_            / ____ \ (_| (_| (_) | |_| | | | | |_
//   \__|____|          /_/    \_\___\___\___/ \__,_|_| |_|\__|

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

    var isFavorite = false
    var isSubscribed = false

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

//   _   ____             _____          _
//  | | |___ \           |  __ \        | |
//  | |_  __) |  ______  | |__) |__  ___| |_
//  | __||__ <  |______| |  ___/ _ \/ __| __|
//  | |_ ___) |          | |  | (_) \__ \ |_
//   \__|____/           |_|   \___/|___/\__|


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
    val media: Media?,
    val domain: String
    // TODO: Add crosspost_parent_list
) : Parcelable, Item(ItemType.Post) {

    @IgnoredOnParcel
    override val depth = 0

    @IgnoredOnParcel
    var isRead = false

    val isImage: Boolean
        get(){
            url?.let {
                val uri = Uri.parse(it)
                uri.lastPathSegment?.let { lastPathSegment ->
                    return lastPathSegment.contains("(.jpg|.png|.svg)".toRegex())
                }
            }
            return false
        }

    val isGif: Boolean
        get(){
            url?.let {
                val uri = Uri.parse(it)
                uri.lastPathSegment?.let { lastPathSegment ->
                    return lastPathSegment.contains(".gif$".toRegex())
                }
            }
            return false
        }

    val videoUrl: String?
        get() {
            return when {
                secureMedia?.redditVideo != null -> secureMedia.redditVideo.hlsUrl
                media?.redditVideo != null -> media.redditVideo.hlsUrl
                preview?.redditVideo != null -> preview.redditVideo.hlsUrl
                else -> null
            }
        }

    @IgnoredOnParcel
    val isGfycat: Boolean = domain == "gfycat.com"

    @IgnoredOnParcel
    val isRedditVideo = domain == "v.redd.it"

    val isGfv: Boolean
        get(){
            url?.let {
                val uri = Uri.parse(it)
                uri.lastPathSegment?.let { lastPathSegment ->
                    return lastPathSegment.contains(".gifv".toRegex())
                }
            }
            return false
        }

    @IgnoredOnParcel
    val shortLink = "http://redd.it/$id"

    val postType: PostType
        get(){
            return when{
                isSelf -> PostType.TEXT
                isImage -> PostType.IMAGE
                videoUrl != null -> PostType.VIDEO
                else -> PostType.URL
            }
        }
}

enum class PostType{
    TEXT,
    IMAGE,
    VIDEO,
    URL
}

@Parcelize
enum class UrlType: Parcelable{
    IMAGE,
    GIF,
    GIFV,
    GFYCAT,
    M3U8,
    LINK
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

//   _   _  _              __  __
//  | | | || |            |  \/  |
//  | |_| || |_   ______  | \  / | ___  ___ ___  __ _  __ _  ___
//  | __|__   _| |______| | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \
//  | |_   | |            | |  | |  __/\__ \__ \ (_| | (_| |  __/
//   \__|  |_|            |_|  |_|\___||___/___/\__,_|\__, |\___|
//                                                     __/ |
//                                                    |___/

@Parcelize
data class Message(
    override val id: String,
    @Json(name = "parent_id")
    val parentId: String?,
    override val name: String,
    val subject: String,
    @Json(name = "was_comment")
    val wasComment: Boolean,
    val author: String,
    @Json(name = "created_utc")
    val created: Long,
    val body: String,
    val dest: String,
    val new: Boolean,
    @Json(name = "subreddit_name_prefixed")
    val subredditNamePrefixed: String?
) : Parcelable, Item(ItemType.Message) {
    @IgnoredOnParcel
    override val depth = 0
}

//   _   _____             _____       _                  _     _ _ _
//  | | | ____|           / ____|     | |                | |   | (_) |
//  | |_| |__    ______  | (___  _   _| |__  _ __ ___  __| | __| |_| |_
//  | __|___ \  |______|  \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|
//  | |_ ___) |           ____) | |_| | |_) | | |  __/ (_| | (_| | | |_
//   \__|____/           |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__|

@Parcelize
data class Subreddit(
    override val name: String,
    @Json(name = "display_name")
    val displayName: String,
    @Json(name = "icon_img")
    val iconImg: String?,
    @Json(name = "title")
    val title: String,
    @Json(name = "banner_img")
    val bannerImg: String?,
    @Json(name = "user_is_subscriber")
    var userSubscribed: Boolean?,
    @Json(name = "public_description")
    val publicDescription: String,
    val url: String,
    @Transient
    var isFavorite: Boolean = false
) : Parcelable, Item(ItemType.Subreddit) {
    @IgnoredOnParcel
    override val depth: Int = 0
    @IgnoredOnParcel
    override val id = name.replace("t5_","")

    fun asSubscription(userId: String) = Subscription(
        "${name}__${userId}",
        displayName,
        displayName.removePrefix("u_"),
        userId,
        (iconImg?.toValidImgUrl() ?: ""),
        url,
        isFavorite,
        if(url.startsWith("/r/", true)){
            SubscriptionType.SUBREDDIT
        } else {
            SubscriptionType.USER
        }
    )
}

fun List<Subreddit>.asSubscriptions(userId: String) = map { it.asSubscription(userId) }

data class SubredditNamesResponse(val names: List<String>)

//   _     __                                          _
//  | |   / /                /\                       | |
//  | |_ / /_    ______     /  \__      ____ _ _ __ __| |
//  | __| '_ \  |______|   / /\ \ \ /\ / / _` | '__/ _` |
//  | |_| (_) |           / ____ \ V  V / (_| | | | (_| |
//   \__|\___/           /_/    \_\_/\_/ \__,_|_|  \__,_|

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

//     __  __       _ _   _        _____          _     _ _ _
//    |  \/  |     | | | (_)      |  __ \        | |   | (_) |
//    | \  / |_   _| | |_ _ ______| |__) |___  __| | __| |_| |_ ___
//    | |\/| | | | | | __| |______|  _  // _ \/ _` |/ _` | | __/ __|
//    | |  | | |_| | | |_| |      | | \ \  __/ (_| | (_| | | |_\__ \
//    |_|  |_|\__,_|_|\__|_|      |_|  \_\___|\__,_|\__,_|_|\__|___/
//
@Parcelize
data class MultiReddit(
    @Json(name = "can_edit")
    val canEdit: Boolean,
    @Json(name = "display_name")
    val displayName: String,
    override val name: String,
    val subreddits: List<SubredditData>,
    val path: String,
    val owner: String,
    @Json(name = "owner_id")
    val ownerId: String,
    @Json(name = "icon_url")
    val iconUrl: String
): Item(ItemType.MultiReddit), Parcelable {

    @IgnoredOnParcel
    override val depth = 0
    @IgnoredOnParcel
    override val id = ""
    @IgnoredOnParcel
    var isFavorite = false

    fun asSubscription() = Subscription(
    "${displayName}__${ownerId.replace("t2_","")}",
        name,
        displayName,
        ownerId.replace("t2_",""),
        iconUrl,
        path,
        isFavorite,
        SubscriptionType.MULTIREDDIT)
}

@Parcelize
data class SubredditData(
    val name: String,
    val data: Subreddit
): Parcelable

fun List<MultiReddit>.asSubscriptions() = map { it.asSubscription() }
