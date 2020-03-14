package dev.gtcl.reddit.network

import android.net.Uri
import android.os.Parcelable
import com.squareup.moshi.Json
import dev.gtcl.reddit.database.ReadListing
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

enum class ListingItemType {
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

sealed class ListingItem(
    val kind: ListingItemType){
    abstract val depth: Int
    abstract val id: String
    abstract val name: String
    var hiddenPoints = 0 // Hide if > 0
}

class ListingResponse(val data: ListingData)

class ListingData(
    val children: List<ListingChild>,
    val after: String?,
    val before: String?
)

sealed class ListingChild(@Json(name="kind") val kind: ListingItemType)

data class PostListing(val data: Post) : ListingChild(ListingItemType.Post) // t3
data class CommentListing(val data: Comment): ListingChild(ListingItemType.Comment) // t1
data class MoreListing(val data: More): ListingChild(ListingItemType.More) // more

// http://patorjk.com/software/taag/#p=display&f=Ivrit&t=t1%20-%20Comment

//     _   _            ____                                     _
//    | |_/ |          / ___|___  _ __ ___  _ __ ___   ___ _ __ | |_
//    | __| |  _____  | |   / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __|
//    | |_| | |_____| | |__| (_) | | | | | | | | | | |  __/ | | | |_
//     \__|_|          \____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__|

@Parcelize
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
): Parcelable,  ListingItem(ListingItemType.Comment)

//     _   _____           ____           _
//    | |_|___ /          |  _ \ ___  ___| |_
//    | __| |_ \   _____  | |_) / _ \/ __| __|
//    | |_ ___) | |_____| |  __/ (_) \__ \ |_
//     \__|____/          |_|   \___/|___/\__|

@Parcelize
data class Post(
    override val name: String,
    override val id: String = name.replace("t3_", ""),
    val saved: Boolean,
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
    val likes: Boolean?,
    val permalink: String,
    val selftext: String,
    @Json(name = "is_self")
    val isSelf: Boolean, // if true, post is a text
    @Json(name = "upvote_ratio")
    val upvoteRatio: Double?,
    val secureMedia: SecureMedia?,
    val preview: Preview?,
    val media: Media?
) : Parcelable, ListingItem(ListingItemType.Post) {

    @IgnoredOnParcel
    override val depth = 0

    fun asReadListing() = ReadListing(this.name)

    fun isPicture(): Boolean{
        url?.let {
            val uri = Uri.parse(it)
            uri.lastPathSegment?.let { lastPathSegment ->
                return lastPathSegment.contains("(.jpg|.png|.gif|.svg)".toRegex())
            }
        }
        return false
    }
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
): ListingItem(ListingItemType.More) {

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