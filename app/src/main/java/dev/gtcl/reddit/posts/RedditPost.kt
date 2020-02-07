package dev.gtcl.reddit.posts

import android.net.Uri
import android.os.Parcelable
import com.squareup.moshi.Json
import dev.gtcl.reddit.database.ReadPost
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RedditPost(
        val name: String,
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
    ) : Parcelable {

        fun asReadPost() = ReadPost(this.name)

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

class PostListingResponse(val data: PostListingData)

class PostListingData(
        val children: List<PostChildrenResponse>,
        val after: String?,
        val before: String?
)

data class PostChildrenResponse(val data: RedditPost)