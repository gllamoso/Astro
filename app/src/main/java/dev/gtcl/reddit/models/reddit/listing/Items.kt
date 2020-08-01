package dev.gtcl.reddit.models.reddit.listing

import android.os.Parcelable
import android.webkit.URLUtil
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.SavedAccount
import dev.gtcl.reddit.database.Subscription
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

sealed class Item(val kind: ItemType) : Parcelable{
    abstract val id: String?
    abstract val name: String
    var hiddenPoints = 0 // Hide if > 0
}

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

// http://patorjk.com/software/taag/#p=display&f=Big&t=t1%20-%20Comment

//   _  __             _____                                     _
//  | |/_ |           / ____|                                   | |
//  | |_| |  ______  | |     ___  _ __ ___  _ __ ___   ___ _ __ | |_
//  | __| | |______| | |    / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __|
//  | |_| |          | |___| (_) | | | | | | | | | | |  __/ | | | |_
//   \__|_|           \_____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__|

@Parcelize
data class Comment( // TODO: Add more properties: all_awardings
    override val name: String,
    override val id: String,
    var depth: Int?,
    val author: String,
    @Json(name="author_fullname")
    val authorFullName: String?,
    val body: String,
    val score: Int,
    @Json(name="created_utc")
    val created: Long,
    val saved: Boolean?,
    var likes: Boolean?,
    var isPartiallyCollapsed: Boolean = false
// TODO: is_submitter
): Item(ItemType.Comment)

//   _   ___                                               _
//  | | |__ \               /\                            | |
//  | |_   ) |  ______     /  \   ___ ___ ___  _   _ _ __ | |_
//  | __| / /  |______|   / /\ \ / __/ __/ _ \| | | | '_ \| __|
//  | |_ / /_            / ____ \ (_| (_| (_) | |_| | | | | |_
//   \__|____|          /_/    \_\___\___\___/ \__,_|_| |_|\__|

@Parcelize
data class Account(
    override val id: String,
    override val name: String,
    @Json(name = "icon_img") val iconImg: String?,
    @Json(name = "link_karma") val linkKarma: Int,
    @Json(name = "comment_karma") val commentKarma: Int,
    @Json(name = "created_utc") val created: Long,
    val subreddit: Subreddit,
    // Additional field
    var refreshToken: String?
) : Item(ItemType.Account) {

    fun getValidProfileImg(): String {
        val imgRegex = "http.+\\.(png|jpg|gif)".toRegex()
        return imgRegex.find(iconImg ?: "")?.value ?: ""
    }

    fun getValidBannerImg(): String {
        val imgRegex = "http.+\\.(png|jpg|gif)".toRegex()
        return imgRegex.find(subreddit.bannerImg ?: "")?.value ?: ""
    }

    fun asDbModel() = SavedAccount(
        id = this.id,
        name = this.name,
        refreshToken = this.refreshToken
    )

    fun asSubscription(userId: String) = Subscription(
        "${subreddit.name}__${userId}",
        "u_${name}",
        name,
        userId,
        iconImg,
        "/user/${name}/",
        false,
        SubscriptionType.USER
    )
}

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
    val domain: String,
    @Json(name = "over_18")
    var nsfw: Boolean,
    var spoiler: Boolean,
    @Json(name = "link_flair_text")
    var flairText: String?
    // TODO: Add crosspost_parent_list, is_submitter
) : Item(ItemType.Post) {

    @IgnoredOnParcel
    var isRead = false

    @IgnoredOnParcel
    val previewVideoUrl: String?
        get() {
            return when {
                secureMedia?.redditVideo != null -> secureMedia.redditVideo.hlsUrl
                media?.redditVideo != null -> media.redditVideo.hlsUrl
                preview?.redditVideo != null -> preview.redditVideo.hlsUrl
                else -> null
            }
        }

    @IgnoredOnParcel
    val shortLink = "http://redd.it/$id"

    @IgnoredOnParcel
    val postType: PostType
        get(){
            return when{
                isSelf -> {
                    if(URLUtil.isValidUrl(selftext)){
                        PostType.TEXT_URL
                    } else{
                        PostType.TEXT
                    }
                }
                GIF_REGEX.matches(url ?: "") -> PostType.GIF
                IMAGE_REGEX.matches(url ?: "") -> PostType.IMAGE
                previewVideoUrl != null || GFYCAT_REGEX.matches(url ?: "") || HLS_REGEX.matches(url ?: "") || GIFV_REGEX.matches(url ?: "") -> PostType.VIDEO
                else -> PostType.URL
            }
        }
}

enum class PostType{
    @SerializedName("self")
    TEXT,
    @SerializedName("self_url")
    TEXT_URL,
    @SerializedName("image")
    IMAGE,
    @SerializedName("videogif")
    GIF,
    @SerializedName("video")
    VIDEO,
    @SerializedName("link")
    URL
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
) : Item(ItemType.Message)

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
    val url: String
) : Item(ItemType.Subreddit) {
    @IgnoredOnParcel
    override val id = name.replace("t5_","")

    @IgnoredOnParcel
    private var isFavorite = false

    fun setFavorite(favorite: Boolean){
        this.isFavorite = favorite
    }

    fun asSubscription(userId: String) = Subscription(
        "${name}__${userId}",
        displayName,
        displayName.removePrefix("u_"),
        userId,
        iconImg?.toValidImgUrl(),
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

//   _     __                                          _
//  | |   / /                /\                       | |
//  | |_ / /_    ______     /  \__      ____ _ _ __ __| |
//  | __| '_ \  |______|   / /\ \ \ /\ / / _` | '__/ _` |
//  | |_| (_) |           / ____ \ V  V / (_| | | | (_| |
//   \__|\___/           /_/    \_\_/\_/ \__,_|_|  \__,_|

class TrophyListingResponse(val data: TrophyListingData)
class TrophyListingData(val trophies: List<TrophyChild>)

@Parcelize
data class Award(
    override val name: String,
    override val id: String?,
    @Json(name = "icon_70") val icon70: String,
    @Json(name = "icon_40") val icon40: String
) : Parcelable, Item(ItemType.Award){
}

//                                        __  __
//     _ __ ___   ___  _ __ ___          |  \/  | ___  _ __ ___
//    | '_ ` _ \ / _ \| '__/ _ \  _____  | |\/| |/ _ \| '__/ _ \
//    | | | | | | (_) | | |  __/ |_____| | |  | | (_) | | |  __/
//    |_| |_| |_|\___/|_|  \___|         |_|  |_|\___/|_|  \___|

@Parcelize
data class More(
    override val name: String,
    override val id: String = name.replace("t1_", ""),
    val depth: Int,
    @Json(name = "parent_id") val parentId: String,
    val children: List<String>,
    var count: Int
): Item(ItemType.More) {

    @IgnoredOnParcel
    private var fetchIndex = 0

    private fun pollChildren(count: Int): List<String>{
        val list = ArrayList<String>()
        while(fetchIndex < children.size && list.size < count){
            list.add(children[fetchIndex++])
        }
        return list
    }

    fun pollChildrenAsValidString(count: Int): String{
        val sb = StringBuilder()
        val children = pollChildren(count)
        for(child in children){
            sb.append("$child,")
        }
        val lastCommaIndex = sb.lastIndexOf(",")
        if(lastCommaIndex > 0 && lastCommaIndex < sb.length){
            sb.deleteCharAt(lastCommaIndex)
        }
        return sb.toString()
    }

    fun lastChildFetched() = fetchIndex == children.size

    fun childrenLeft() = children.size - fetchIndex

    @IgnoredOnParcel
    val isContinueThreadLink = id == "_"
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
    val iconUrl: String,
    val visibility: Visibility,
    @Json(name = "description_md")val description: String
): Item(ItemType.MultiReddit) {
    @IgnoredOnParcel
    override val id = ""

    @IgnoredOnParcel
    private var isFavorite = false
    fun setFavorite(favorite: Boolean){
        this.isFavorite = favorite
    }

    fun asSubscription() = Subscription(
        "${name}__${ownerId.replace("t2_","")}",
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
    val data: Subreddit?
): Parcelable

@Parcelize
data class MultiRedditUpdate(
    @SerializedName("description_md") val description: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("icon_img") val iconImg: String? = null,
    @SerializedName("key_color") val keyColor: String? = null,
    val subreddits: List<SubredditData>? = null,
    val visibility: Visibility? = null
): Parcelable {
    override fun toString(): String = Gson().toJson(this)
}

fun List<MultiReddit>.asSubscriptions() = map { it.asSubscription() }