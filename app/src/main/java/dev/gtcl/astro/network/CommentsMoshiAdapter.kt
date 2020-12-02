package dev.gtcl.astro.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import dev.gtcl.astro.models.reddit.listing.*
import java.util.*
import kotlin.collections.LinkedHashMap

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
    fun toJson(commentPage: CommentPage) = "{$commentPage}" // Unused

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
        if (jsonReader.nextName() != "kind") {
            throw RuntimeException("Did not find 'kind' key.")
        }
        jsonReader.skipValue()
        if (jsonReader.nextName() != "data") {
            throw RuntimeException("Did not find 'data' key.")
        }
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            if (jsonReader.nextName() == "children") {
                jsonReader.beginArray()
                jsonReader.beginObject()
                if (jsonReader.nextName() != "kind" || jsonReader.nextString() != "t3") {
                    throw RuntimeException("Unable to find posted. Expected to find 'kind' = 't3'")
                }
                if (jsonReader.nextName() != "data") {
                    throw RuntimeException("Did not find 'kind' key.")
                }
                post = getPostFromData(jsonReader)
                jsonReader.endObject() // end child
                jsonReader.endArray()
            } else {
                jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        jsonReader.endObject()

        return post!!
    }

    private fun getPostFromData(jsonReader: JsonReader): Post {
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
        var selftextHtml: String? = null
        var isSelf: Boolean? = null
        var upvoteRatio: Double? = null
        var secureMedia: SecureMedia? = null
        var preview: Preview? = null
        var media: Media? = null
        var mediaMetaData: Map<String, MediaMetadata>? = null
        var galleryData: GalleryData? = null
        var domain: String? = null
        var nsfw: Boolean? = null
        var spoiler: Boolean? = null
        var linkFlairText: String? = null
        var linkFlairTemplateId: String? = null
        var crossPostParentList: MutableList<Post>? = null
        var crosspostable: Boolean? = null
        var totalAwards: Int? = null
        var awards: List<Award>? = null
        var sendReplies: Boolean? = null
        var canModPost: Boolean? = null
        var stickied: Boolean? = null
        var pinned: Boolean? = null
        var locked: Boolean? = null
        var removedBy: String? = null
        var flairRichtext: List<FlairRichtext>? = null
        var flairColor: String? = null
        var flairTextColor: String? = null

        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
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
                    if (jsonReader.peek() == JsonReader.Token.BOOLEAN) {
                        likes = jsonReader.nextBoolean()
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "permalink" -> {
                    permalink = jsonReader.nextString()
                }
                "selftext" -> {
                    selftext = jsonReader.nextString()
                }
                "selftext_html" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        selftextHtml = jsonReader.nextString()
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "is_self" -> {
                    isSelf = jsonReader.nextBoolean()
                }
                "upvote_ratio" -> {
                    upvoteRatio = jsonReader.nextDouble()
                }
                "secure_media" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        secureMedia = getSecureMedia(jsonReader)
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "media" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        media = getMedia(jsonReader)
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "media_metadata" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        mediaMetaData = getMediaMetadata(jsonReader)
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "gallery_data" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        galleryData = getGalleryData(jsonReader)
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
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        linkFlairText = jsonReader.nextString()
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "link_flair_template_id" -> {
                    linkFlairTemplateId = jsonReader.nextString()
                }
                "crosspost_parent_list" -> {
                    jsonReader.beginArray()
                    crossPostParentList = mutableListOf()
                    while (jsonReader.peek() != JsonReader.Token.END_ARRAY) {
                        crossPostParentList.add(getPostFromData(jsonReader))
                    }
                    jsonReader.endArray()
                }
                "is_crosspostable" -> {
                    crosspostable = jsonReader.nextBoolean()
                }
                "total_awards_received" -> {
                    totalAwards = jsonReader.nextInt()
                }
                "all_awardings" -> {
                    val mutableAwards = mutableListOf<Award>()
                    jsonReader.beginArray()
                    while(jsonReader.hasNext()){
                        mutableAwards.add(getAward(jsonReader))
                    }
                    jsonReader.endArray()
                    awards = mutableAwards.toList()
                }
                "send_replies" -> {
                    sendReplies = jsonReader.nextBoolean()
                }
                "can_mod_post" -> {
                    canModPost = jsonReader.nextBoolean()
                }
                "stickied" -> {
                    stickied = jsonReader.nextBoolean()
                }
                "pinned" -> {
                    pinned = jsonReader.nextBoolean()
                }
                "locked" -> {
                    locked = jsonReader.nextBoolean()
                }
                "removed_by_category" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        removedBy = jsonReader.nextString()
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "link_flair_richtext" ->  {
                    jsonReader.beginArray()
                    flairRichtext = mutableListOf()
                    while (jsonReader.hasNext()) {
                        flairRichtext.add(getFlairRichtext(jsonReader))
                    }
                    jsonReader.endArray()
                }
                "link_flair_background_color" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        flairColor = jsonReader.nextString()
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "link_flair_text_color" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        flairTextColor = jsonReader.nextString()
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
            selftextHtml = selftextHtml,
            isSelf = isSelf!!,
            upvoteRatio = upvoteRatio,
            secureMedia = secureMedia,
            preview = preview,
            media = media,
            mediaMetadata = mediaMetaData,
            galleryData = galleryData,
            domain = domain!!,
            nsfw = nsfw!!,
            spoiler = spoiler!!,
            flairText = linkFlairText,
            linkFlairTemplateId = linkFlairTemplateId,
            crosspostParentList = crossPostParentList,
            isCrosspostable = crosspostable!!,
            totalAwards = totalAwards!!,
            awards = awards!!,
            sendReplies = sendReplies!!,
            canModPost = canModPost!!,
            stickied = stickied!!,
            pinned = pinned!!,
            locked = locked!!,
            removedBy = removedBy,
            flairRichtext = flairRichtext,
            flairColor = flairColor,
            flairTextColor = flairTextColor
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
        var images: List<PreviewImages>? = null
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when(jsonReader.nextName()){
                "reddit_video_preview" -> {
                    jsonReader.beginObject()
                    while (jsonReader.hasNext()) {
                        if (jsonReader.nextName() == "hls_url") {
                            val hlsUrl = jsonReader.nextString()
                            videoPreview = RedditVideo(hlsUrl)
                        } else {
                            jsonReader.skipValue()
                        }
                    }
                    jsonReader.endObject()
                }
                "images" -> {
                    val parsedImages = mutableListOf<PreviewImages>()
                    jsonReader.beginArray()
                    while(jsonReader.hasNext()){
                        parsedImages.add(getPreviewImages(jsonReader))
                    }
                    jsonReader.endArray()
                    images = parsedImages.toList()
                }
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        return Preview(images!!, videoPreview)
    }

    private fun getPreviewImages(jsonReader: JsonReader): PreviewImages {
        var source: PreviewImage? = null
        var resolutions: List<PreviewImage>? = null
        var variants: ImageVariant? = null
        jsonReader.beginObject()
        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "source" -> source = getPreviewImage(jsonReader)
                "resolutions" -> {
                    jsonReader.beginArray()
                    val previewImages = mutableListOf<PreviewImage>()
                    while(jsonReader.hasNext()){
                        previewImages.add(getPreviewImage(jsonReader))
                    }
                    jsonReader.endArray()
                    resolutions = previewImages.toList()
                }
                "variants" -> variants = getImageVariant(jsonReader)
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        return PreviewImages(source!!, resolutions!!, variants)
    }

    private fun getImageVariant(jsonReader: JsonReader): ImageVariant {
        var nsfw: PreviewImages? = null
        jsonReader.beginObject()

        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "nsfw" -> nsfw = getPreviewImages(jsonReader)
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()

        return ImageVariant(nsfw)
    }

    private fun getPreviewImage(jsonReader: JsonReader): PreviewImage {
        var url: String? = null
        var width: Int? = null
        var height: Int? = null
        jsonReader.beginObject()
        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "url" -> url = jsonReader.nextString()
                "width" -> width = jsonReader.nextInt()
                "height" -> height = jsonReader.nextInt()
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()

        return PreviewImage(url!!, width!!, height!!)
    }

    private fun getAward(jsonReader: JsonReader): Award {
        var count: Int? = null
        var icons: List<AwardIcon>? = null
        var iconUrl: String? = null
        jsonReader.beginObject()

        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "count" -> count = jsonReader.nextInt()
                "resized_static_icons" -> {
                    val mutableIcons = mutableListOf<AwardIcon>()
                    jsonReader.beginArray()
                    while(jsonReader.hasNext()){
                        mutableIcons.add(getAwardIcon(jsonReader))
                    }
                    jsonReader.endArray()
                    icons = mutableIcons.toList()
                }
                "icon_url" -> iconUrl = jsonReader.nextString()
                else -> jsonReader.skipValue()
            }
        }

        jsonReader.endObject()
        return Award(count!!, icons!!, iconUrl!!)
    }

    private fun getAwardIcon(jsonReader: JsonReader): AwardIcon {
        var url: String? = null
        jsonReader.beginObject()

        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "url" -> url = jsonReader.nextString()
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()

        return AwardIcon(url!!)
    }

    private fun getMediaMetadata(jsonReader: JsonReader): Map<String, MediaMetadata> {
        val map = LinkedHashMap<String, MediaMetadata>()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            val id = jsonReader.nextName()
            var mimeType: String? = null
            var previews: List<GalleryPreview>? = null
            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                when(jsonReader.nextName()){
                    "m" -> {
                        mimeType = jsonReader.nextString()
                    }
                    "p" -> {
                        jsonReader.beginArray()
                        val mutablePreviews = mutableListOf<GalleryPreview>()
                        while(jsonReader.hasNext()){
                            mutablePreviews.add(getGalleryPreview(jsonReader))
                        }
                        jsonReader.endArray()
                        previews = mutablePreviews.toList()
                    }
                    else -> jsonReader.skipValue()
                }
            }
            jsonReader.endObject()
            map[id] = MediaMetadata(id, mimeType, previews)
        }
        jsonReader.endObject()
        return map
    }

    private fun getGalleryPreview(jsonReader: JsonReader): GalleryPreview {
        var url: String? = null

        jsonReader.beginObject()
        while(jsonReader.hasNext()){
            when(jsonReader.nextName()){
                "u" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        url = jsonReader.nextString()
                    } else {
                        jsonReader.skipValue()
                    }
                }
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()

        return GalleryPreview(url)
    }

    private fun getGalleryData(jsonReader: JsonReader): GalleryData {
        jsonReader.beginObject()
        val list = mutableListOf<GalleryItem>()

        while (jsonReader.hasNext()) {
            if (jsonReader.nextName() == "items") {
                jsonReader.beginArray()
                while (jsonReader.hasNext()) {
                    jsonReader.beginObject()
                    var caption: String? = null
                    var mediaId: String? = null
                    while (jsonReader.hasNext()) {
                        when (jsonReader.nextName()) {
                            "caption" -> caption = jsonReader.nextString()
                            "media_id" -> mediaId = jsonReader.nextString()
                            else -> jsonReader.skipValue()
                        }
                    }
                    list.add(GalleryItem(caption, mediaId!!))
                    jsonReader.endObject()
                }
                jsonReader.endArray()
            } else {
                jsonReader.skipValue()
            }
        }

        jsonReader.endObject()
        return GalleryData(list)
    }

    // COMMENTS

    private fun getCommentsFromListing(jsonReader: JsonReader, depth: Int): List<Item> {
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

    private fun addChildren(jsonReader: JsonReader, depth: Int, comments: MutableList<Item>) {
        while (jsonReader.hasNext()) {
            jsonReader.beginObject()
            var isMore = false
            while (jsonReader.hasNext()) {
                when (jsonReader.nextName()) {
                    "kind" -> {
                        if (jsonReader.nextString() == "more") {
                            isMore = true
                        }
                    }
                    "data" -> {
                        if (isMore) {
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

    private fun addCommentToList(jsonReader: JsonReader, depth: Int, comments: MutableList<Item>) {
        jsonReader.beginObject()
        var name: String? = null
        var id: String? = null
        var author: String? = null
        var authorFullName: String? = null
        var authorCakeday: Boolean? = null
        var body: String? = null
        var bodyHtml: String? = null
        var score: Int? = null
        var scoreHidden: Boolean? = null
        var created: Long? = null
        var saved: Boolean? = null
        var likes: Boolean? = null
        var replies: List<Item>? = null
        var authorFlairText: String? = null
        var totalAwards: Int? = null
        var awards: List<Award>? = null
        var permalink: String? = null
        var linkPermalink: String? = null
        var parentId: String? = null
        var context: String? = null
        var new: Boolean? = null
        var subreddit: String? = null
        var subredditPrefixed: String? = null
        var linkTitle: String? = null
        var isSubmitter: Boolean? = null
        var locked: Boolean? = null
        var stickied: Boolean? = null
        val authorFlairRichtext = mutableListOf<FlairRichtext>()

        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "name" -> {
                    name = jsonReader.nextString()
                }
                "id" -> {
                    id = jsonReader.nextString()
                }
                "author" -> {
                    author = jsonReader.nextString()
                }
                "author_fullname" -> {
                    authorFullName = jsonReader.nextString()
                }
                "author_cakeday" -> {
                    authorCakeday = jsonReader.nextBoolean()
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
                    if (jsonReader.peek() == JsonReader.Token.NULL) {
                        jsonReader.skipValue()
                    } else {
                        likes = jsonReader.nextBoolean()
                    }
                }
                "saved" -> {
                    saved = jsonReader.nextBoolean()
                }
                "author_flair_text" -> {
                    if (jsonReader.peek() == JsonReader.Token.NULL) {
                        jsonReader.skipValue()
                    } else {
                        authorFlairText = jsonReader.nextString()
                    }
                }
                "author_flair_richtext" -> {
                    jsonReader.beginArray()
                    while (jsonReader.hasNext()) {
                        authorFlairRichtext.add(getFlairRichtext(jsonReader))
                    }
                    jsonReader.endArray()
                }
                "total_awards_received" -> {
                    totalAwards = jsonReader.nextInt()
                }
                "all_awardings" -> {
                    val mutableAwards = mutableListOf<Award>()
                    jsonReader.beginArray()
                    while(jsonReader.hasNext()){
                        mutableAwards.add(getAward(jsonReader))
                    }
                    jsonReader.endArray()
                    awards = mutableAwards.toList()
                }
                "permalink" -> {
                    permalink = jsonReader.nextString()
                }
                "link_permalink" -> {
                    linkPermalink = jsonReader.nextString()
                }
                "context" -> {
                    if (jsonReader.peek() != JsonReader.Token.NULL) {
                        context = jsonReader.nextString()
                    } else {
                        jsonReader.skipValue()
                    }
                }
                "parent_id" -> {
                    parentId = jsonReader.nextString()
                }
                "subreddit" -> {
                    subreddit = jsonReader.nextString()
                }
                "subreddit_name_prefixed" -> {
                    subredditPrefixed = jsonReader.nextString()
                }
                "new" -> {
                    new = jsonReader.nextBoolean()
                }
                "link_title" -> {
                    linkTitle = jsonReader.nextString()
                }
                "is_submitter" -> {
                    isSubmitter = jsonReader.nextBoolean()
                }
                "locked" -> {
                    locked = jsonReader.nextBoolean()
                }
                "stickied" -> {
                    stickied = jsonReader.nextBoolean()
                }
                else -> {
                    jsonReader.skipValue()
                }
            }
        }
        jsonReader.endObject()

        val comment = Comment(
            name = name ?: return,
            id = id ?: name.replace("t1_", ""),
            depth = depth,
            author = author ?: return,
            authorFullName = authorFullName,
            authorCakeday = authorCakeday,
            body = body ?: return,
            bodyHtml = bodyHtml ?: return,
            score = score ?: return,
            scoreHidden = scoreHidden,
            created = created ?: return,
            new = new,
            saved = saved,
            likes = likes,
            authorFlairText = authorFlairText,
            flairRichtext = authorFlairRichtext,
            totalAwards = totalAwards,
            awards = awards,
            permalink = permalink ?: return,
            linkPermalink = linkPermalink,
            context = context,
            parentId = parentId,
            subreddit = subreddit ?: return,
            subredditPrefixed = subredditPrefixed ?: return,
            linkTitle = linkTitle,
            isSubmitter = isSubmitter,
            locked = locked,
            stickied = stickied
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

        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
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
                    while (jsonReader.hasNext()) children.add(jsonReader.nextString())
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

    private fun getFlairRichtext(jsonReader: JsonReader): FlairRichtext {
        jsonReader.beginObject()
        var tag: String? = null
        var type: String? = null
        var text: String? = null
        var url: String? = null

        while (jsonReader.hasNext()) {
            when (val field = jsonReader.nextName()) {
                "a" -> tag = jsonReader.nextString()
                "e" -> type = jsonReader.nextString()
                "t" -> text = jsonReader.nextString()
                "u" -> url = jsonReader.nextString()
                else -> throw IllegalArgumentException("Invalid Author Richtext Field: $field")
            }
        }

        jsonReader.endObject()
        return FlairRichtext(tag, type!!, text, url)
    }

}