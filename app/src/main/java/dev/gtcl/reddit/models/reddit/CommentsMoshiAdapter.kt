package dev.gtcl.reddit.models.reddit

import android.util.JsonToken
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import dev.gtcl.reddit.models.reddit.listing.*
import java.lang.RuntimeException
import java.util.*

data class CommentPage(
    val post: Post,
    val comments: List<Item>
)

data class MoreComments(
    val position: Int,
    val comments: List<Item>
)

class CommentsMoshiAdapter {

    @ToJson
    fun toJson(commentPage: CommentPage) = "{}" // UNUSED

    @FromJson
    fun getCommentsPageInfo(jsonReader: JsonReader): CommentPage {
        jsonReader.beginArray()
        val post = getPostFromListing(jsonReader)
        val comments = getCommentsFromListing(jsonReader, 0)
        jsonReader.endArray()
        return CommentPage(post, comments)
    }

    // POST

    private fun getPostFromListing(jsonReader: JsonReader): Post {
        var post: Post? = null
        jsonReader.beginObject()
        if(jsonReader.nextName() != "kind") {
            throw RuntimeException("Did not find 'kind' key.")
        }
        jsonReader.skipValue()
        if(jsonReader.nextName() != "data") {
            throw RuntimeException("Did not find 'data' key.")
        }
        jsonReader.beginObject()
        while(jsonReader.hasNext()){
            if(jsonReader.nextName() == "children"){
                jsonReader.beginArray()
                jsonReader.beginObject()
                if(jsonReader.nextName() != "kind" || jsonReader.nextString() != "t3") {
                    throw RuntimeException("Unable to find posted. Expected to find 'kind' = 't3'")
                }
                if(jsonReader.nextName() != "data") {
                    throw RuntimeException("Did not find 'kind' key.")
                }
                post = getPostFromData(jsonReader)
                jsonReader.endObject() // end child
                jsonReader.endArray()
            }
            else {
                jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        jsonReader.endObject()

        return post!!
    }

    private fun getPostFromData(jsonReader: JsonReader) : Post {
        jsonReader.beginObject()
        var name: String? = null
        var saved: Boolean? = null
        var hidden: Boolean? = null
        var title: String? = null
        var score: Int? = null
        var author: String? = null
        var subreddit: String? = null
        var numComments: Int? = null
        var created: Long? = null
        var thumbnail: String? = null
        var url: String? = null
        var likes: Boolean? = null
        var permalink: String? = null
        var selftext: String? = null
        var isSelf: Boolean? = null
        var upvoteRatio: Double? = null
        var secureMedia: SecureMedia? = null
        var preview: Preview? = null
        var media: Media? = null
        var domain: String? = null
        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "name" -> {
                    name = jsonReader.nextString()
                }
                "saved" -> {
                    saved = jsonReader.nextBoolean()
                }
                "hidden" -> {
                    hidden = jsonReader.nextBoolean()
                }
                "title" -> {
                    title = jsonReader.nextString()
                }
                "score" -> {
                    score = jsonReader.nextInt()
                }
                "author" -> {
                    author = jsonReader.nextString()
                }
                "subreddit" -> {
                    subreddit = jsonReader.nextString()
                }
                "num_comments" -> {
                    numComments = jsonReader.nextInt()
                }
                "created_utc" -> {
                    created = jsonReader.nextLong()
                }
                "thumbnail" -> {
                    thumbnail = jsonReader.nextString()
                }
                "url" -> {
                    url = jsonReader.nextString()
                }
                "likes" -> {
                    if(jsonReader.peek() == JsonReader.Token.BOOLEAN) {
                        likes = jsonReader.nextBoolean()
                    }
                    else {
                        jsonReader.skipValue()
                    }
                }
                "permalink" -> {
                    permalink = jsonReader.nextString()
                }
                "selftext" -> {
                    selftext = jsonReader.nextString()
                }
                "is_self" -> {
                    isSelf = jsonReader.nextBoolean()
                }
                "upvote_ratio" -> {
                    upvoteRatio = jsonReader.nextDouble()
                }
                "secure_media" -> {
                    if(jsonReader.peek() != JsonReader.Token.NULL) {
                        secureMedia = getSecureMedia(jsonReader)
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "media" -> {
                    if(jsonReader.peek() != JsonReader.Token.NULL) {
                        media = getMedia(jsonReader)
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "preview" -> {
                    preview = getPreview(jsonReader)
                }
                "domain" -> {
                    domain = jsonReader.nextString()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject() // end "data"

        return Post(
            name = name!!,
            saved = saved!!,
            title = title!!,
            score = score!!,
            author = author!!,
            subreddit = subreddit!!,
            numComments = numComments!!,
            created = created!!,
            thumbnail = thumbnail,
            url = url,
            likes = likes,
            hidden = hidden!!,
            permalink = permalink!!,
            selftext = selftext!!,
            isSelf = isSelf!!,
            upvoteRatio = upvoteRatio,
            secureMedia = secureMedia,
            preview = preview,
            media = media,
            domain = domain!!
        )
    }

    private fun getSecureMedia(jsonReader: JsonReader): SecureMedia {
        var video: RedditVideo? = null
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            if (jsonReader.nextName() == "reddit_video") {
                jsonReader.beginObject()
                while (jsonReader.hasNext()) {
                    if (jsonReader.nextName() == "hls_url") {
                        val hlsUrl = jsonReader.nextString()
                        video = RedditVideo(hlsUrl)
                    } else {
                        jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()
            } else {
                jsonReader.skipValue()
            }
        }
        jsonReader.endObject()

        return SecureMedia(video)
    }

    private fun getMedia(jsonReader: JsonReader): Media {
        var video: RedditVideo? = null
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            if (jsonReader.nextName() == "reddit_video") {
                jsonReader.beginObject()
                while (jsonReader.hasNext()) {
                    if (jsonReader.nextName() == "hls_url") {
                        val hlsUrl = jsonReader.nextString()
                        video = RedditVideo(hlsUrl)
                    } else {
                        jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()
            } else {
                jsonReader.skipValue()
            }
        }
        jsonReader.endObject()

        return Media(video)
    }

    private fun getPreview(jsonReader: JsonReader): Preview {
        var videoPreview: RedditVideo? = null
        jsonReader.beginObject()
        while(jsonReader.hasNext()){
            if(jsonReader.nextName() == "reddit_video_preview"){
                jsonReader.beginObject()
                while(jsonReader.hasNext()){
                    if(jsonReader.nextName() == "hls_url"){
                        val hlsUrl = jsonReader.nextString()
                        videoPreview = RedditVideo(hlsUrl)
                    } else {
                        jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()
            } else {
                jsonReader.skipValue()
            }
        }
        jsonReader.endObject()

        return Preview(videoPreview)
    }

    // COMMENTS

    private fun getCommentsFromListing(jsonReader: JsonReader, depth: Int): List<Item> { // TODO: Make cleaner
        jsonReader.beginObject()
        val comments = mutableListOf<Item>()

        while (jsonReader.hasNext()) {
            if (jsonReader.nextName() == "data") {
                jsonReader.beginObject()
                while (jsonReader.hasNext()) {
                    if (jsonReader.nextName() == "children") {
                        if (jsonReader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                            jsonReader.beginArray()
                            addChildren(jsonReader, depth, comments)
                            jsonReader.endArray()
                        } else {
                            jsonReader.skipValue()
                        }
                    } else {
                        jsonReader.skipValue()
                    }
                }
                jsonReader.endObject()
            } else {
                jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        return comments
    }

    private fun addChildren(jsonReader: JsonReader, depth: Int, comments: MutableList<Item>){
        while (jsonReader.hasNext()) {
            jsonReader.beginObject()
            var isMore = false
            while (jsonReader.hasNext()) {
                when(jsonReader.nextName()){
                    "kind" -> {
                        if(jsonReader.nextString() == "more") {
                            isMore = true
                        }
                    }
                    "data" -> {
                        if(isMore) {
                            comments.add(getMore(jsonReader))
                        } else {
                            addCommentToList(jsonReader, depth, comments)
                        }
                    }
                    else -> {
                        jsonReader.skipValue()
                    }
                }
            }
            jsonReader.endObject()
        }
    }

    private fun addCommentToList(jsonReader: JsonReader, depth: Int, comments: MutableList<Item>){
        jsonReader.beginObject()
        var name: String? = null
        var authorFullName: String? = null
        var author: String? = null
        var body: String? = null
        var score: Int? = null
        var created: Long? = null
        var likes: Boolean? = null
        var saved: Boolean? = null
        var replies: List<Item>? = null
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "name" -> {
                    name = jsonReader.nextString()
                }
                "author" -> {
                    author = jsonReader.nextString()
                }
                "author_fullname" -> {
                    authorFullName = jsonReader.nextString()
                }
                "body" -> {
                    body = jsonReader.nextString()
                }
                "score" -> {
                    score = jsonReader.nextInt()
                }
                "created_utc" -> {
                    created = jsonReader.nextLong()
                }
                "replies" -> {
                    if (jsonReader.peek() == JsonReader.Token.BEGIN_OBJECT)
                        replies = getCommentsFromListing(jsonReader, depth + 1)
                    else jsonReader.skipValue()
                }
                "likes" -> {
                    if(jsonReader.peek() == JsonReader.Token.NULL){
                        jsonReader.skipValue()
                    } else {
                        likes = jsonReader.nextBoolean()
                    }
                }
                "saved" -> {
                    saved = jsonReader.nextBoolean()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject()

        val comment = Comment(
            name = name!!,
            id = "",
            depth = depth,
            author = author!!,
            authorFullName = authorFullName,
            body = body!!,
            score = score!!,
            created = created!!,
            likes =  likes,
            saved = saved
        )

        comments.add(comment)
        replies?.let {
            comments.addAll(it)
        }
    }

    private fun getMore(jsonReader: JsonReader): More {
        jsonReader.beginObject()

        var name: String? = null
        var depth: Int? = null
        var parentId: String? = null
        val children = LinkedList<String>()
        var count: Int? = null

        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "name" -> {
                    name = jsonReader.nextString()
                }
                "count" -> {
                    count = jsonReader.nextInt()
                }
                "depth" -> {
                    depth = jsonReader.nextInt()
                }
                "parent_id" -> {
                    parentId = jsonReader.nextString()
                }
                "children" -> {
                    jsonReader.beginArray()
                    while(jsonReader.hasNext()) children.add(jsonReader.nextString())
                    jsonReader.endArray()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }

        jsonReader.endObject()
        return More(
            name = name!!,
            depth = depth!!,
            parentId = parentId!!,
            children = children,
            count = count!!
        )
    }

}