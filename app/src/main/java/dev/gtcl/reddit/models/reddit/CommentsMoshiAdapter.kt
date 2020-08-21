package dev.gtcl.reddit.models.reddit

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import dev.gtcl.reddit.models.reddit.listing.*
import kotlinx.android.parcel.Parcelize
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
        var authorFullname: String? = null
        var subreddit: String? = null
        var subredditPrefixed: String? = null
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
        var nsfw: Boolean? = null
        var spoiler: Boolean? = null
        var linkFlairText: String? = null
        var crossPostParentList: MutableList<Post>? = null
        var gildings: Gildings? = null
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
                "author_fullname" -> {
                    authorFullname = jsonReader.nextString()
                }
                "subreddit" -> {
                    subreddit = jsonReader.nextString()
                }
                "subreddit_name_prefixed" -> {
                    subredditPrefixed = jsonReader.nextString()
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
                "over_18" -> {
                    nsfw = jsonReader.nextBoolean()
                }
                "spoiler" -> {
                    spoiler = jsonReader.nextBoolean()
                }
                "link_flair_text" -> {
                    if(jsonReader.peek() != JsonReader.Token.NULL) {
                        linkFlairText = jsonReader.nextString()
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "crosspost_parent_list" -> {
                    jsonReader.beginArray()
                    crossPostParentList = mutableListOf()
                    while(jsonReader.peek() != JsonReader.Token.END_ARRAY){
                        crossPostParentList.add(getPostFromData(jsonReader))
                    }
                    jsonReader.endArray()
                }
                "gildings" -> {
                    if(jsonReader.peek() != JsonReader.Token.NULL) {
                        gildings = getGildings(jsonReader)
                    } else {
                        jsonReader.skipValue()
                    }
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
            authorFullName = authorFullname,
            subreddit = subreddit!!,
            subredditPrefixed = subredditPrefixed!!,
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
            domain = domain!!,
            nsfw = nsfw!!,
            spoiler = spoiler!!,
            flairText = linkFlairText,
            crosspostParentList = crossPostParentList,
            gildings = gildings
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

    private fun getGildings(jsonReader: JsonReader): Gildings {
        var silver: Int? = null
        var gold: Int? = null
        var platinum: Int? = null

        jsonReader.beginObject()
        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "gid_1" -> {
                    silver = jsonReader.nextInt()
                }
                "gid_2" -> {
                    gold = jsonReader.nextInt()
                }
                "gid_3" -> {
                    platinum = jsonReader.nextInt()
                }
            }
        }
        jsonReader.endObject()

        return Gildings(silver, gold, platinum)
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
        var author: String? = null
        var authorFullName: String? = null
        var body: String? = null
        var bodyHtml: String? = null
        var score: Int? = null
        var scoreHidden: Boolean? = null
        var created: Long? = null
        var saved: Boolean? = null
        var likes: Boolean? = null
        var replies: List<Item>? = null
        var authorFlairText: String? = null
        var permalink: String? = null
        var linkPermalink: String? = null
        var subreddit: String? = null
        var subredditPrefixed: String? = null
        var linkTitle: String? = null
        var isSubmitter: Boolean? = null
        val authorFlairRichtext = mutableListOf<AuthorFlairRichtext>()

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
                "body_html" -> {
                    bodyHtml = jsonReader.nextString()
                }
                "score" -> {
                    score = jsonReader.nextInt()
                }
                "score_hidden" -> {
                    scoreHidden = jsonReader.nextBoolean()
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
                "author_flair_text" -> {
                    if(jsonReader.peek() == JsonReader.Token.NULL){
                        jsonReader.skipValue()
                    } else {
                        authorFlairText = jsonReader.nextString()
                    }
                }
                "author_flair_richtext" -> {
                    jsonReader.beginArray()
                    while(jsonReader.hasNext()){
                        authorFlairRichtext.add(getAuthorFlairRichtext(jsonReader))
                    }
                    jsonReader.endArray()
                }
                "permalink" -> {
                    permalink = jsonReader.nextString()
                }
                "link_permalink" -> {
                    linkPermalink = jsonReader.nextString()
                }
                "subreddit" -> {
                    subreddit = jsonReader.nextString()
                }
                "subreddit_name_prefixed" -> {
                    subredditPrefixed = jsonReader.nextString()
                }
                "link_title" -> {
                    linkTitle = jsonReader.nextString()
                }
                "is_submitter" ->{
                    isSubmitter = jsonReader.nextBoolean()
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
            bodyHtml = bodyHtml!!,
            score = score!!,
            scoreHidden = scoreHidden!!,
            created = created!!,
            saved = saved!!,
            likes =  likes,
            authorFlairText = authorFlairText,
            authorFlairRichtext = authorFlairRichtext,
            permalink = permalink!!,
            linkPermalink = linkPermalink,
            context = null,
            subreddit = subreddit!!,
            subredditPrefixed = subredditPrefixed!!,
            linkTitle = linkTitle,
            isSubmitter = isSubmitter
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

    private fun getAuthorFlairRichtext(jsonReader: JsonReader): AuthorFlairRichtext{
        jsonReader.beginObject()
        var tag: String? = null
        var type: String? = null
        var text: String? = null
        var url: String? = null

        while(jsonReader.hasNext()){
            when(val field = jsonReader.nextName()){
                "a" -> tag = jsonReader.nextString()
                "e" -> type = jsonReader.nextString()
                "t" -> text = jsonReader.nextString()
                "u" -> url = jsonReader.nextString()
                else -> throw IllegalArgumentException("Invalid Author Richtext Field: $field")
            }
        }

        jsonReader.endObject()
        return AuthorFlairRichtext(tag, type!!, text, url)
    }

}