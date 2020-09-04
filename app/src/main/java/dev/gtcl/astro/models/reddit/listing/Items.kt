package dev.gtcl.astro.models.reddit.listing

import android.os.Parcelable
import android.text.Html
import androidx.room.Ignore
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import dev.gtcl.astro.*
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.models.reddit.MediaURL
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.util.*

sealed class Item(val kind: ItemType) : Parcelable {
    abstract val id: String?
    abstract val name: String
}

enum class ItemType {
    @Json(name = "t1")
    Comment,

    @Json(name = "t2")
    Account,

    @Json(name = "t3")
    Post,

    @Json(name = "t4")
    Message,

    @Json(name = "t5")
    Subreddit,

    @Json(name = "t6")
    Award,

    @Json(name = "more")
    More,

    @Json(name = "LabeledMulti")
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
data class Comment(
    override val name: String,
    override val id: String,
    var depth: Int?,
    val author: String,
    @Json(name = "author_fullname")
    val authorFullName: String?,
    @Json(name = "author_cakeday")
    val authorCakeday: Boolean?,
    val body: String,
    @Json(name = "body_html")
    val bodyHtml: String,
    var score: Int,
    @Json(name = "score_hidden")
    var scoreHidden: Boolean?,
    @Json(name = "created_utc")
    val created: Long,
    var new: Boolean?,
    var saved: Boolean?,
    var likes: Boolean?,
    @Json(name = "author_flair_text")
    val authorFlairText: String?,
    @Json(name = "author_flair_richtext")
    val authorFlairRichtext: List<AuthorFlairRichtext>?,
    val gildings: Gildings?,
    val permalink: String?,
    @Json(name = "link_permalink")
    val linkPermalink: String?,
    val context: String?,
    @Json(name = "parent_id")
    val parentId: String?,
    val subreddit: String,
    @Json(name = "subreddit_name_prefixed")
    val subredditPrefixed: String,
    @Json(name = "link_title")
    val linkTitle: String?,
    @Json(name = "is_submitter")
    val isSubmitter: Boolean?,
    val stickied: Boolean?,
    val locked: Boolean?,
    var isCollapsed: Boolean = false
) : Item(ItemType.Comment) {

    @IgnoredOnParcel
    val linkTitleFormatted: CharSequence? = if (linkTitle != null) {
        Html.fromHtml(linkTitle, Html.FROM_HTML_MODE_COMPACT)
    } else {
        null
    }

    @IgnoredOnParcel
    val bodyFormatted: CharSequence = Html.fromHtml(body, Html.FROM_HTML_MODE_COMPACT)

    @IgnoredOnParcel
    val permalinkWithRedditDomain = "https://www.reddit.com$permalink"

    @IgnoredOnParcel
    val deleted = author == "[deleted]"

    fun updateScore(vote: Vote){
        when(vote){
            Vote.UPVOTE -> {
                when(likes){
                    true -> score--
                    false -> score += 2
                    null -> score++

                }
                likes = if(likes != true){
                    true
                } else {
                    null
                }
            }
            Vote.DOWNVOTE -> {
                when(likes){
                    true -> score -= 2
                    false -> score++
                    null -> score--
                }
                likes = if(likes != false){
                    false
                } else {
                    null
                }
            }
            Vote.UNVOTE -> {
                when(likes){
                    true -> score--
                    false -> score++
                }
                likes = null
            }
        }
    }
}

@Parcelize
data class AuthorFlairRichtext(
    @Json(name = "a")
    val tag: String?,
    @Json(name = "e")
    val type: String,
    @Json(name = "t")
    val text: String?,
    @Json(name = "u")
    val url: String?
) : Parcelable

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
    @Json(name = "is_friend") var isFriend: Boolean?,
    var refreshToken: String?
) : Item(ItemType.Account) {

    fun getValidProfileImg(): String {
        val imgRegex = "http.+\\.(png|jpg|gif)".toRegex()
        return imgRegex.find(iconImg ?: "")?.value ?: ""
    }

    fun getValidBannerImg(): String {
        return IMAGE_REGEX.find(subreddit.bannerImg ?: "")?.value ?: ""
    }

    fun asDbModel() = SavedAccount(
        id = this.id,
        name = this.name,
        refreshToken = this.refreshToken
    )

    @IgnoredOnParcel
    val fullId = "t2_$id"
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
    var score: Int,
    val author: String,
    @Json(name = "author_fullname")
    val authorFullName: String?,
    val subreddit: String,
    @Json(name = "subreddit_name_prefixed")
    val subredditPrefixed: String,
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
    @Json(name = "media_metadata")
    val mediaMetadata: Map<String, MediaMetadata>?,
    @Json(name = "gallery_data")
    val galleryData: GalleryData?,
    val domain: String,
    @Json(name = "over_18")
    var nsfw: Boolean,
    var spoiler: Boolean,
    @Json(name = "link_flair_text")
    var flairText: String?,
    @Json(name = "link_flair_template_id")
    var linkFlairTemplateId: String?,
    @Json(name = "crosspost_parent_list")
    val crosspostParentList: List<Post>?,
    @Json(name = "is_crosspostable")
    val isCrosspostable: Boolean,
    val gildings: Gildings?,
    @Json(name = "send_replies")
    var sendReplies: Boolean,
    @Json(name = "can_mod_post")
    val canModPost: Boolean,
    @Json(name = "stickied")
    val stickied: Boolean,
    val pinned: Boolean,
    val locked: Boolean
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
                crosspostParentList?.get(0)?.previewVideoUrl != null -> crosspostParentList[0].previewVideoUrl
                else -> null
            }
        }

    @IgnoredOnParcel
    val shortLink = "https://redd.it/$id"

    fun getFlairTextFormatted(): CharSequence? = if (flairText != null) {
        Html.fromHtml(flairText, Html.FROM_HTML_MODE_COMPACT)
    } else {
        null
    }

    @IgnoredOnParcel
    val permalinkWithRedditDomain = "https://www.reddit.com$permalink"

    @IgnoredOnParcel
    val titleFormatted: CharSequence = Html.fromHtml(title, Html.FROM_HTML_MODE_COMPACT)

    @IgnoredOnParcel
    val deleted = author == "[deleted]"

    @IgnoredOnParcel
    val urlType = url?.getUrlType()

    @IgnoredOnParcel
    val galleryAsMediaItems: List<MediaURL>? = if(galleryData != null){
        mutableListOf<MediaURL>().apply{
            for(id: String in galleryData.items.map { it.mediaId }){
                val metaData = mediaMetadata!![id] ?: error("MetaData is null")
                val mimeType = metaData.mimeType
                val extension = when{
                    mimeType.endsWith("png") -> "png"
                    mimeType.endsWith("jpg") -> "jpg"
                    mimeType.endsWith("png") -> "png"
                    mimeType.endsWith("svg") -> "svg"
                    mimeType.endsWith("jpeg") -> "jpeg"
                    mimeType.endsWith("gif") -> "gif"
                    else -> null
                }
                if(extension.isNullOrBlank()){
                    continue
                }
                val mediaType = if(extension == "gif"){
                    MediaType.GIF
                } else {
                    MediaType.PICTURE
                }
                add(MediaURL("https://i.redd.it/$id.$extension", mediaType))
            }
        }
    } else {
        null
    }

    fun updateScore(vote: Vote){
        when(vote){
            Vote.UPVOTE -> {
                when(likes){
                    true -> score--
                    false -> score += 2
                    null -> score++

                }
                likes = if(likes != true){
                    true
                } else {
                    null
                }
            }
            Vote.DOWNVOTE -> {
                when(likes){
                    true -> score -= 2
                    false -> score++
                    null -> score--
                }
                likes = if(likes != false){
                    false
                } else {
                    null
                }
            }
            Vote.UNVOTE -> {
                when(likes){
                    true -> score--
                    false -> score++
                }
                likes = null
            }
        }
    }
}

enum class PostType {
    @SerializedName("self")
    TEXT,

    @SerializedName("link")
    URL,

    @SerializedName("crosspost")
    CROSSPOST
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
) : Parcelable

@Parcelize
data class RedditVideo(
    @Json(name = "hls_url")
    val hlsUrl: String
) : Parcelable

@Parcelize
data class Gildings(
    @Json(name = "gid_1")
    val silver: Int?,
    @Json(name = "gid_2")
    val gold: Int?,
    @Json(name = "gid_3")
    val platinum: Int?
) : Parcelable

@Parcelize
data class  GalleryData(
    val items: List<GalleryItem>
) : Parcelable

@Parcelize
data class GalleryItem(
    val caption: String?,
    @Json(name = "media_id")
    val mediaId: String
) : Parcelable

@Parcelize
data class MediaMetadata(
    val id: String,
    @Json(name = "m")
    val mimeType: String
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
    var new: Boolean,
    @Json(name = "subreddit_name_prefixed")
    val subredditNamePrefixed: String?
) : Item(ItemType.Message) {

    @IgnoredOnParcel
    val bodyFormatted: CharSequence = Html.fromHtml(body, Html.FROM_HTML_MODE_COMPACT)
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
    @Json(name = "display_name_prefixed")
    val displayNamePrefixed: String,
    @Json(name = "icon_img")
    val iconImg: String?,
    val title: String,
    @Json(name = "community_icon")
    val communityIcon: String?,
    @Json(name = "banner_img")
    val bannerImg: String?,
    @Json(name = "banner_background_image")
    val bannerBackgroundImg: String?,
    @Json(name = "user_is_subscriber")
    var userSubscribed: Boolean?,
    @Json(name = "public_description")
    val publicDescription: String,
    val description: String?,
    val url: String
) : Item(ItemType.Subreddit) {
    @IgnoredOnParcel
    override val id = name.replace("t5_", "")

    @IgnoredOnParcel
    var isFavorite = false

    fun asSubscription(userId: String) = Subscription(
        "${name}__${userId}",
        displayName,
        displayName.removePrefix("u_"),
        userId,
        icon,
        url,
        isFavorite,
        if (url.startsWith("/r/", true)) {
            SubscriptionType.SUBREDDIT
        } else {
            SubscriptionType.USER
        }
    )

    @IgnoredOnParcel
    val icon: String? = when {
        !iconImg.isNullOrBlank() -> iconImg.toValidImgUrl()
        !communityIcon.isNullOrBlank() -> communityIcon.toValidImgUrl()
        else -> null
    }

    @IgnoredOnParcel
    val banner: String?
        get() {
            return when {
                !bannerImg.isNullOrBlank() -> bannerImg.toValidImgUrl()
                !bannerBackgroundImg.isNullOrBlank() -> bannerBackgroundImg.toValidImgUrl()
                else -> null
            }
        }

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
) : Parcelable, Item(ItemType.Award)

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
) : Item(ItemType.More) {

    @IgnoredOnParcel
    private var fetchIndex = 0

    @IgnoredOnParcel
    private var previousFetchIndex = 0

    private fun pollChildren(count: Int): List<String> {
        val list = ArrayList<String>()
        previousFetchIndex = fetchIndex
        while (fetchIndex < children.size && list.size < count) {
            list.add(children[fetchIndex++])
        }
        return list
    }

    fun pollChildrenAsValidString(count: Int): String {
        val sb = StringBuilder()
        val children = pollChildren(count)
        for (child in children) {
            sb.append("$child,")
        }
        val lastCommaIndex = sb.lastIndexOf(",")
        if (lastCommaIndex > 0 && lastCommaIndex < sb.length) {
            sb.deleteCharAt(lastCommaIndex)
        }
        return sb.toString()
    }

    fun undoChildrenPoll() {
        fetchIndex = previousFetchIndex
    }

    val lastChildFetched: Boolean
        get() = fetchIndex == children.size

    val childrenLeft: Int
        get() = children.size - fetchIndex

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
    @Json(name = "description_md") val description: String
) : Item(ItemType.MultiReddit) {
    @IgnoredOnParcel
    override val id = ""

    @IgnoredOnParcel
    private var isFavorite = false
    fun setFavorite(favorite: Boolean) {
        this.isFavorite = favorite
    }

    fun asSubscription() = Subscription(
        "${name}__${ownerId.replace("t2_", "")}",
        name,
        displayName,
        ownerId.replace("t2_", ""),
        iconUrl,
        path,
        isFavorite,
        SubscriptionType.MULTIREDDIT
    )
}

@Parcelize
data class SubredditData(
    val name: String,
    val data: Subreddit?
) : Parcelable

@Parcelize
data class MultiRedditUpdate(
    @SerializedName("description_md") val description: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("icon_img") val iconImg: String? = null,
    @SerializedName("key_color") val keyColor: String? = null,
    val subreddits: List<SubredditData>? = null,
    val visibility: Visibility? = null
) : Parcelable {
    override fun toString(): String = Gson().toJson(this)
}

fun List<MultiReddit>.asSubscriptions() = map { it.asSubscription() }