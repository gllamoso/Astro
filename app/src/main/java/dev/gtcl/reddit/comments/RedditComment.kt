package dev.gtcl.reddit.comments

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import dev.gtcl.reddit.posts.*
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

data class CommentPage(
    val post: RedditPost,
    val comments: List<CommentItem>
)

open class CommentItem(
    open val id: String,
    open var depth: Int
)

data class Comment( // TODO: Add more properties: saved, liked, all_awardings
    override val id: String,
    override var depth: Int,
    val author: String,
    val authorFullName: String?,
    val body: String,
    val score: Int,
    val created: Long,
    var isTotallyCollapsed: Boolean = false,
    var isPartiallyCollapsed: Boolean = false
): CommentItem(id, depth)

data class More(
    override val id: String,
    override var depth: Int,
    val parentId: String,
    val children: List<String>,
    var count: Int
    ): CommentItem(id, depth) {

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

data class MoreComments(
    val position: Int,
    val depth: Int,
    val comments: List<CommentItem>
)

val AUTHOR_REGEX = "data-author=\"[A-Za-z0-9_\\-]+\"".toRegex()
val AUTHOR_FULLNAME_REGEX = "data-author-fullname=\"[A-Za-z0-9_\\-]+\"".toRegex()
val SCORE_LIKES_REGEX = "\"score likes\" title=\"[0-9\\-]+\"".toRegex()
val MORE_CHILDREN_REGEX = "morechildren\\([A-Za-z0-9,_' ]+\\)".toRegex()

data class Child(
    val kind: String,
    val parent: String,
    val content: String,
    val contentText: String,
    val link: String,
    val contentHTML: String,
    val id: String
    ) {

    fun convertToComment(depth: Int): Comment{
        if(contentText == "[deleted]")
            return Comment(id, depth, "[deleted]", null, contentText, 0, 0)

        val authorResult = AUTHOR_REGEX.find(content)
        val authorFullNameResult = AUTHOR_FULLNAME_REGEX.find(content)
        val scoreResult = SCORE_LIKES_REGEX.find(content)

        if(authorResult == null) throw RuntimeException("Could not find 'author'. Comment id: $id")
        if(authorFullNameResult == null) throw RuntimeException("Could not find 'authorFullNameResult'. Comment id: $id")
        if(scoreResult == null) throw RuntimeException("Could not find 'scoreResult'. Comment id: $id")

        return Comment(id.replace("t1_", ""), depth, authorResult.value.split("\"")[1], authorFullNameResult.value.split("\"")[1], contentText, scoreResult.value.split("\"")[3].toInt(), 99)
    }

    fun convertToMore(depth: Int): More {
        val childrenResult = MORE_CHILDREN_REGEX.find(content)
            ?: throw RuntimeException("Exception in 'convertToMore'. Could not find 'children'. Comment id: $id")

        val children = childrenResult.value.split("',")[2].trim().removePrefix("'").split(",")
        return More(id.replace("t1_", ""), depth, parent, children, children.size)
    }
}

fun List<Child>.convertChildrenToCommentItems(startingDepth: Int): List<CommentItem> {
    val results = mutableListOf<CommentItem>()
    val depthMap = HashMap<String, Int>()
    for(child in this){
        val parentDepth = depthMap.getOrElse(child.parent){null}
        val newDepth = if(parentDepth == null) startingDepth else parentDepth + 1
        depthMap[child.id] = newDepth
        if(child.kind == "t1")
            results.add(child.convertToComment(newDepth))
        else if(child.kind == "more")
            results.add(child.convertToMore(newDepth))
    }

    return results
}

class CommentAdapter {

    @FromJson
    fun getCommentsPageInfo(jsonReader: JsonReader): CommentPage { // TODO: Finish
        jsonReader.beginArray()
        val post = getRedditPost(jsonReader)
        val comments = getComments(jsonReader, 0)
        jsonReader.endArray()
        return CommentPage(post, comments)
    }

    private fun getRedditPost(jsonReader: JsonReader): RedditPost { // TODO: Finish
        var post: RedditPost? = null
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
                jsonReader.beginObject()
                var name: String? = null
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
                                jsonReader.beginObject()
                                while (jsonReader.hasNext()) {
                                    if (jsonReader.nextName() == "reddit_video") {
                                        jsonReader.beginObject()
                                        while (jsonReader.hasNext()) {
                                            if (jsonReader.nextName() == "hls_url") {
                                                val hlsUrl = jsonReader.nextString()
                                                val video = RedditVideo(hlsUrl)
                                                secureMedia = SecureMedia(video)
                                            } else jsonReader.skipValue()
                                        }
                                        jsonReader.endObject()
                                    } else jsonReader.skipValue()
                                }
                                jsonReader.endObject()
                            } else jsonReader.skipValue()
                        }
                        "media" -> {
                            if(jsonReader.peek() != JsonReader.Token.NULL) {
                                jsonReader.beginObject()
                                while (jsonReader.hasNext()) {
                                    if (jsonReader.nextName() == "reddit_video") {
                                        jsonReader.beginObject()
                                        while (jsonReader.hasNext()) {
                                            if (jsonReader.nextName() == "hls_url") {
                                                val hlsUrl = jsonReader.nextString()
                                                val video = RedditVideo(hlsUrl)
                                                media = Media(video)
                                            } else jsonReader.skipValue()
                                        }
                                        jsonReader.endObject()
                                    } else jsonReader.skipValue()
                                }
                                jsonReader.endObject()
                            } else jsonReader.skipValue()
                        }
                        "preview" -> {
                            jsonReader.beginObject()
                            while(jsonReader.hasNext()){
                                if(jsonReader.nextName() == "reddit_video_preview"){
                                    jsonReader.beginObject()
                                    while(jsonReader.hasNext()){
                                        if(jsonReader.nextName() == "hls_url"){
                                            val hlsUrl = jsonReader.nextString()
                                            val videoPreview = RedditVideo(hlsUrl)
                                            preview = Preview(videoPreview)
                                        } else jsonReader.skipValue()
                                    }
                                    jsonReader.endObject()
                                } else jsonReader.skipValue()
                            }
                            jsonReader.endObject()
                        }
                        else -> jsonReader.skipValue()
                    }
                }
                post = RedditPost(name!!, title!!, score!!, author!!, subreddit!!, numComments!!, created!!, thumbnail, url, likes, permalink!!, selftext!!, isSelf!!, upvoteRatio, secureMedia, preview, media)
                jsonReader.endObject() // end "data"
                jsonReader.endObject() // end child
                jsonReader.endArray()
            }
            else jsonReader.skipValue()
        }
        jsonReader.endObject()
        jsonReader.endObject()

        return post!!
    }

    @FromJson
    fun getComments(jsonReader: JsonReader): List<CommentItem> {
        jsonReader.beginArray()
        jsonReader.skipValue() // skip Post details
        val comments = getComments(jsonReader, 0)
        jsonReader.endArray()
        return comments
    }

    private fun getComments(jsonReader: JsonReader, depth: Int): List<CommentItem> {
        jsonReader.beginObject()
        val items = mutableListOf<CommentItem>()

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
                                            if(isMore)
                                                items.add(getMoreInfo(jsonReader))
                                            else {
                                                var id: String? = null
                                                var authorFullName: String? = null
                                                var author: String? = null
                                                var body: String? = null
                                                var score: Int? = null
                                                var created: Long? = null
                                                var replies: List<CommentItem>? = null
                                                while (jsonReader.hasNext()) {
                                                    when (jsonReader.nextName()) {
                                                        "id" -> id = jsonReader.nextString()
                                                        "author" -> author = jsonReader.nextString()
                                                        "author_fullname" -> authorFullName = jsonReader.nextString()
                                                        "body" -> body = jsonReader.nextString()
                                                        "score" -> score = jsonReader.nextInt()
                                                        "created_utc" -> created = jsonReader.nextLong()
                                                        "replies" -> {
                                                            if (jsonReader.peek() == JsonReader.Token.BEGIN_OBJECT)
                                                                replies = getComments(jsonReader, depth + 1)
                                                            else jsonReader.skipValue()
                                                        }
                                                        else -> jsonReader.skipValue()
                                                    }
                                                }
                                                if(id == null) throw RuntimeException("Exception in 'getComments'. Did not find 'id'.")
                                                if(author == null) throw RuntimeException("Exception in 'getComments'. Did not find 'author'.")
                                                if(body == null) throw RuntimeException("Exception in 'getComments'. Did not find 'body'.")
                                                if(score == null) throw RuntimeException("Exception in 'getComments'. Did not find 'score'.")
                                                if(created == null) throw RuntimeException("Exception in 'getComments'. Did not find 'created'.")
                                                val comment = Comment(id, depth, author, authorFullName, body, score, created)
                                                items.add(comment)
                                                replies?.let {
                                                    items.addAll(it)
                                                }
                                            }
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

    private fun getMoreInfo(jsonReader: JsonReader): More{
        var id: String? = null
        var depth: Int? = null
        var parentId: String? = null
        val children = LinkedList<String>()
        var count: Int? = null

        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "id" -> id = jsonReader.nextString()
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

        if(id == null) throw RuntimeException("Exception in 'getMoreInfo'. Did not find 'id'.")
        if(depth == null) throw RuntimeException("Exception in 'getMoreInfo'. Did not find 'depth'.")
        if(parentId == null) throw RuntimeException("Exception in 'getMoreInfo'. Did not find 'parentId'.")
        if(count == null) throw RuntimeException("Exception in 'getMoreInfo'. Did not find 'count'.")

        return More(id, depth, parentId, children, count)
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

        if(parent == null) throw RuntimeException("Exception in 'getCommentChild'. Did not find 'parent'.")
        if(content == null) throw RuntimeException("Exception in 'getCommentChild'. Did not find 'content'.")
        if(contentText == null) throw RuntimeException("Exception in 'getCommentChild'. Did not find 'contentText'.")
        if(link == null) throw RuntimeException("Exception in 'getCommentChild'. Did not find 'link'.")
        if(contentHTML == null) throw RuntimeException("Exception in 'getCommentChild'. Did not find 'contentHTML'.")
        if(id == null) throw RuntimeException("Exception in 'getCommentChild'. Did not find 'id'.")

        return Child(kind, parent, content, contentText, link, contentHTML, id)
    }

}