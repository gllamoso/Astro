package dev.gtcl.reddit.comments

import android.annotation.SuppressLint
import android.os.Build
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import dev.gtcl.reddit.network.*
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap

data class CommentPage(
    val post: Post,
    val comments: List<ListingItem>
)

data class MoreComments(
    val position: Int,
    val depth: Int,
    val comments: List<ListingItem>
)

val AUTHOR_REGEX = "data-author=\"[A-Za-z0-9_\\-]+\"".toRegex()
val AUTHOR_FULLNAME_REGEX = "data-author-fullname=\"[A-Za-z0-9_\\-]+\"".toRegex()
val SCORE_LIKES_REGEX = "\"score likes\" title=\"[0-9\\-]+\"".toRegex()
val MORE_CHILDREN_REGEX = "morechildren\\([A-Za-z0-9,_' ]+\\)".toRegex()
val TIME_REGEX = "time title=\"[A-Za-z0-9: ]+ UTC\"".toRegex()
const val DATE_TIME_PATTERN = "EEE MMM dd HH:mm:ss yyyy"
val URL_REGEX = "href=\"[a-z/_0-9]+\"".toRegex()

data class Child(
    val kind: String,
    val parent: String,
    val content: String,
    val contentText: String,
    val link: String,
    val contentHTML: String,
    val id: String
    ) {

    fun isComment() = kind == "t1"
    fun isMore() = kind == "more" && !content.contains("&gt;continue this thread&lt;")
    fun isContinueThread() = kind == "more" && content.contains("&gt;continue this thread&lt;")

    @SuppressLint("SimpleDateFormat")
    fun toComment(depth: Int): Comment {
        if(contentText == "[deleted]")
            return Comment(id, "[deleted]", depth, "[deleted]", null, contentText, 0, 0)

        val authorResult = AUTHOR_REGEX.find(content)
        val authorFullNameResult = AUTHOR_FULLNAME_REGEX.find(content)
        val scoreResult = SCORE_LIKES_REGEX.find(content)
        val time = TIME_REGEX.find(content)
        val timeString = time!!.value.replace("time title=\"", "").replace("  ", " 0").replace(" UTC\"", "")

        val created = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
            val dateTime = LocalDateTime.parse(timeString, formatter)
            val zdt = dateTime.atZone(ZoneId.of("UTC"))
            zdt.toInstant().toEpochMilli() / 1000
        } else {
            val formatter = SimpleDateFormat(DATE_TIME_PATTERN)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val date = formatter.parse(timeString)
            date!!.time / 1000
        }

        return Comment(
            id = id.replace("t1_", ""),
            name = id,
            depth = depth,
            author = if (authorResult != null) authorResult.value.split("\"")[1] else "[removed]",
            authorFullName = if (authorFullNameResult != null) authorFullNameResult.value.split("\"")[1] else "[removed]",
            body = contentText,
            score = if(scoreResult != null) scoreResult.value.split("\"")[3].toInt() else Integer.MIN_VALUE,
            created = created)
    }

    fun toMore(depth: Int): More {
        val childrenResult = MORE_CHILDREN_REGEX.find(content)
            ?: throw RuntimeException("Exception in 'convertToMore'. Could not find 'children'. Comment id: $id")

        val children = childrenResult.value.split("',")[2].trim().removePrefix("'").split(",")
        return More(id.replace("t1_", ""), id, depth, parent, children, children.size)
    }

//    fun toContinueThread(depth: Int): ContinueThread {
//        val url = URL_REGEX.find(content)!!.value.replace("href=\"/", "").replace("\"", "")
//        return ContinueThread(id, depth, url)
//    }
}

fun List<Child>.convertChildrenToCommentItems(startingDepth: Int): List<ListingItem> {
    val results = mutableListOf<ListingItem>()
    val depthMap = HashMap<String, Int>()
    for(child in this){
        val parentDepth = depthMap.getOrElse(child.parent){null}
        val newDepth = if(parentDepth == null) startingDepth else parentDepth + 1
        depthMap[child.id] = newDepth
        when {
            child.isComment() -> results.add(child.toComment(newDepth))
            child.isMore() -> results.add(child.toMore(newDepth))
//            child.isContinueThread() -> results.add(child.toContinueThread(newDepth))
        }
    }

    return results
}

class CommentAdapter {

    @FromJson
    fun getCommentsPageInfo(jsonReader: JsonReader): CommentPage { // TODO: Finish
        jsonReader.beginArray()
        val post = getPostFromListing(jsonReader)
        val comments = getCommentsFromListing(jsonReader, 0)
        jsonReader.endArray()
        return CommentPage(post, comments)
    }

    // POST

    private fun getPostFromListing(jsonReader: JsonReader): Post { // TODO: Finish
        var post: Post? = null
        jsonReader.beginObject()
        if(jsonReader.nextName() != "kind") throw RuntimeException("Did not find 'kind' key.")
        jsonReader.skipValue()
        if(jsonReader.nextName() != "data") throw RuntimeException("Did not find 'data' key.")
        jsonReader.beginObject()
        while(jsonReader.hasNext()){
            if(jsonReader.nextName() == "children"){
                jsonReader.beginArray()
                jsonReader.beginObject()
                if(jsonReader.nextName() != "kind" || jsonReader.nextString() != "t3") throw RuntimeException("Unable to find posted. Expected to find 'kind' = 't3'")
                if(jsonReader.nextName() != "data") throw RuntimeException("Did not find 'kind' key.")
                post = getPostFromData(jsonReader)
                jsonReader.endObject() // end child
                jsonReader.endArray()
            }
            else jsonReader.skipValue()
        }
        jsonReader.endObject()
        jsonReader.endObject()

        return post!!
    }

    private fun getPostFromData(jsonReader: JsonReader) : Post{
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
        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "name" -> name = jsonReader.nextString()
                "saved" -> saved = jsonReader.nextBoolean()
                "hidden" -> hidden = jsonReader.nextBoolean()
                "title" -> title = jsonReader.nextString()
                "score" -> score = jsonReader.nextInt()
                "author" -> author = jsonReader.nextString()
                "subreddit" -> subreddit = jsonReader.nextString()
                "num_comments" -> numComments = jsonReader.nextInt()
                "created_utc" -> created = jsonReader.nextLong()
                "thumbnail" -> thumbnail = jsonReader.nextString()
                "url" -> url = jsonReader.nextString()
                "likes" -> {
                    if(jsonReader.peek() == JsonReader.Token.BOOLEAN)
                        likes = jsonReader.nextBoolean()
                    else
                        jsonReader.skipValue()
                }
                "permalink" -> permalink = jsonReader.nextString()
                "selftext" -> selftext = jsonReader.nextString()
                "is_self" -> isSelf = jsonReader.nextBoolean()
                "upvote_ratio" -> upvoteRatio = jsonReader.nextDouble()
                "secure_media" -> {
                    if(jsonReader.peek() != JsonReader.Token.NULL) {
                        secureMedia = getSecureMedia(jsonReader)
                    } else jsonReader.skipValue()
                }
                "media" -> {
                    if(jsonReader.peek() != JsonReader.Token.NULL) {
                        media = getMedia(jsonReader)
                    } else jsonReader.skipValue()
                }
                "preview" -> {
                    preview = getPreview(jsonReader)
                }
                else -> jsonReader.skipValue()
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
            media = media)
    }

    private fun getSecureMedia(jsonReader: JsonReader): SecureMedia{
        var hlsUrl: String? = null
        var video: RedditVideo? = null
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            if (jsonReader.nextName() == "reddit_video") {
                jsonReader.beginObject()
                while (jsonReader.hasNext()) {
                    if (jsonReader.nextName() == "hls_url") {
                        hlsUrl = jsonReader.nextString()
                        video = RedditVideo(hlsUrl)
                    } else jsonReader.skipValue()
                }
                jsonReader.endObject()
            } else jsonReader.skipValue()
        }
        jsonReader.endObject()

        return SecureMedia(video)
    }

    private fun getMedia(jsonReader: JsonReader): Media{
        var hlsUrl: String? = null
        var video: RedditVideo? = null
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            if (jsonReader.nextName() == "reddit_video") {
                jsonReader.beginObject()
                while (jsonReader.hasNext()) {
                    if (jsonReader.nextName() == "hls_url") {
                        hlsUrl = jsonReader.nextString()
                        video = RedditVideo(hlsUrl)
                    } else jsonReader.skipValue()
                }
                jsonReader.endObject()
            } else jsonReader.skipValue()
        }
        jsonReader.endObject()

        return Media(video)
    }

    private fun getPreview(jsonReader: JsonReader): Preview{
        var hlsUrl: String? = null
        var videoPreview: RedditVideo? = null
        jsonReader.beginObject()
        while(jsonReader.hasNext()){
            if(jsonReader.nextName() == "reddit_video_preview"){
                jsonReader.beginObject()
                while(jsonReader.hasNext()){
                    if(jsonReader.nextName() == "hls_url"){
                        hlsUrl = jsonReader.nextString()
                        videoPreview = RedditVideo(hlsUrl)
                    } else jsonReader.skipValue()
                }
                jsonReader.endObject()
            } else jsonReader.skipValue()
        }
        jsonReader.endObject()

        return Preview(videoPreview)
    }

    // COMMENTS

    private fun getCommentsFromListing(jsonReader: JsonReader, depth: Int): List<ListingItem> {
        jsonReader.beginObject()
        val items = mutableListOf<ListingItem>()

        while (jsonReader.hasNext()) {
            if (jsonReader.nextName() == "data") {
                jsonReader.beginObject()
                while (jsonReader.hasNext()) {
                    if (jsonReader.nextName() == "children") {
                        if (jsonReader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                            jsonReader.beginArray()
                            while (jsonReader.hasNext()) {
                                jsonReader.beginObject()
                                var isMore = false
                                while (jsonReader.hasNext()) {
                                    when(jsonReader.nextName()){
                                        "kind" -> {
                                            if(jsonReader.nextString() == "more")
                                                isMore = true
                                        }
                                        "data" -> {
                                            jsonReader.beginObject()
                                            if(isMore) items.add(getMore(jsonReader))
                                            else addCommentsToList(jsonReader, depth, items)
                                            jsonReader.endObject()
                                        }
                                        else -> jsonReader.skipValue()
                                    }
                                }
                                jsonReader.endObject()
                            }
                            jsonReader.endArray()
                        } else jsonReader.skipValue()
                    } else jsonReader.skipValue()
                }
                jsonReader.endObject()
            } else jsonReader.skipValue()

        }

        jsonReader.endObject()
        return items
    }

    private fun addCommentsToList(jsonReader: JsonReader, depth: Int, comments: MutableList<ListingItem>){
        var name: String? = null
        var authorFullName: String? = null
        var author: String? = null
        var body: String? = null
        var score: Int? = null
        var created: Long? = null
        var replies: List<ListingItem>? = null
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "name" -> name = jsonReader.nextString()
                "author" -> author = jsonReader.nextString()
                "author_fullname" -> authorFullName = jsonReader.nextString()
                "body" -> body = jsonReader.nextString()
                "score" -> score = jsonReader.nextInt()
                "created_utc" -> created = jsonReader.nextLong()
                "replies" -> {
                    if (jsonReader.peek() == JsonReader.Token.BEGIN_OBJECT)
                        replies = getCommentsFromListing(jsonReader, depth + 1)
                    else jsonReader.skipValue()
                }
                else -> jsonReader.skipValue()
            }
        }

        val comment =  Comment(name = name!!, depth = depth, author = author!!, authorFullName = authorFullName, body = body!!, score = score!!, created = created!!)

        comments.add(comment)
        replies?.let {
            comments.addAll(it)
        }
    }

    private fun getMore(jsonReader: JsonReader): More{
        var name: String? = null
        var depth: Int? = null
        var parentId: String? = null
        val children = LinkedList<String>()
        var count: Int? = null

        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "name" -> name = jsonReader.nextString()
                "count" -> count = jsonReader.nextInt()
                "depth" -> depth = jsonReader.nextInt()
                "parent_id" -> parentId = jsonReader.nextString()
                "children" -> {
                    jsonReader.beginArray()
                    while(jsonReader.hasNext()) children.add(jsonReader.nextString())
                    jsonReader.endArray()
                }
                else -> jsonReader.skipValue()
            }
        }

        return More(name = name!!, depth = depth!!, parentId = parentId!!, children = children, count = count!!)
    }

    @FromJson
    fun getMoreComments(jsonReader: JsonReader):List<Child> {
        val resultList = mutableListOf<Child>()
        jsonReader.beginObject()
            if(jsonReader.nextName() != "json") throw RuntimeException("Exception in 'getMoreComments'. Did not find 'json' object.")
            jsonReader.beginObject() // begin "json"
                if(jsonReader.nextName() != "errors") throw RuntimeException("Exception in 'getMoreComments'. Did not find 'error' array.")
                jsonReader.skipValue() // skip "errors"
                if(jsonReader.nextName() != "data") throw RuntimeException("Exception in 'getMoreComments'. Did not find 'data' object.")
                jsonReader.beginObject() // begin "data"
                    if(jsonReader.nextName() != "things") throw RuntimeException("Exception in 'getMoreComments'. Did not find 'things' array.")
                    jsonReader.beginArray()
                        while(jsonReader.hasNext())
                            resultList.add(getCommentChild(jsonReader))
                    jsonReader.endArray()
                jsonReader.endObject()
            jsonReader.endObject()
        jsonReader.endObject()

        return resultList
    }

    private fun getCommentChild(jsonReader: JsonReader): Child {
        jsonReader.beginObject()
        if(jsonReader.nextName() != "kind") throw RuntimeException("Exception in 'getCommentChild'. Unable to find kind.")
        val kind = jsonReader.nextString()
        if(jsonReader.nextName() != "data") throw RuntimeException("Exception in 'getCommentChild'. Wasn't able to find 'data' key.")
        jsonReader.beginObject()

        var parent: String? = null
        var content: String? = null
        var contentText: String? = null
        var link: String? = null
        var contentHTML: String? = null
        var id: String? = null

        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "parent" -> parent = jsonReader.nextString()
                "content" -> content = jsonReader.nextString()
                "contentText" -> contentText = jsonReader.nextString()
                "link" -> link = jsonReader.nextString()
                "contentHTML" -> contentHTML = jsonReader.nextString()
                "id" -> id = jsonReader.nextString()
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        jsonReader.endObject()

        return Child(kind, parent!!, content!!, contentText!!, link!!, contentHTML!!, id!!)
    }

}