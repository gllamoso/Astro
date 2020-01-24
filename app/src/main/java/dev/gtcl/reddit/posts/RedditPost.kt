package dev.gtcl.reddit.posts

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
        val upvoteRatio: Double?
    ) : Parcelable

fun RedditPost.asReadPost() = ReadPost(this.name)

// Reddit API Response

class PostListingResponse(val data: PostListingData)

class PostListingData(
        val children: List<PostChildrenResponse>,
        val after: String?,
        val before: String?
)

data class PostChildrenResponse(val data: RedditPost)