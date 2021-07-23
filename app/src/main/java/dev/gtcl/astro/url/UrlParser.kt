package dev.gtcl.astro.url

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

val GFYCAT_REGEX = "(?:(?:http[s]?://)?(?:\\w+\\.)?gfycat\\.com)/(?:gifs/detail/)?(\\w+).*".toRegex()
val REDGIFS_REGEX = "(?:(?:http[s]?://)?(?:\\w+\\.)?redgifs\\.com)/watch/(\\w+).*".toRegex()
val IMAGE_REGEX = "http[s]?://.+\\.(?:jpg|png|jpeg|webp)".toRegex()
val GIFV_REGEX = "http[s]?://.+\\.gifv(?:/.+|/)?".toRegex()
val GIF_REGEX = "http[s]?://.+\\.gif(?:/.+|/)?".toRegex()
val HLS_REGEX = "http[s]?://.+/HLSPlaylist\\.m3u8.*".toRegex()
val REDDIT_VIDEO_REGEX = "(?:(?:http[s]?://)?v\\.redd\\.it)/(\\w+)".toRegex()
val STANDARD_VIDEO_REGEX = "http[s]?://.+\\.(?:mp4).*".toRegex()
val IMGUR_ALBUM_REGEX = "(?:(?:http[s]?://)?(?:\\w+\\.)?imgur\\.com)/(?:a|gallery)/(\\w+)/?".toRegex()
val IMGUR_IMAGE_REGEX = "(?:(?:http[s]?://)?(?:\\w+\\.)?imgur\\.com)/(\\w+)/?".toRegex()
val SUBREDDIT_REGEX = "(?:(?:http[s]?://)?(?:\\w+\\.)?reddit\\.com/|/)?r/([A-Za-z0-9_.]+)/?".toRegex()
val REDDIT_USER_REGEX = "(?:(?:http[s]?://)?(?:\\w+\\.)?reddit\\.com/|/)?(?:u|user)/([A-Za-z0-9_.]+)/?".toRegex()
val REDDIT_GALLERY_REGEX = "(?:http[s]?://)?\\w+\\.reddit\\.com/gallery/(\\w+)/?.*".toRegex()
val REDDIT_THREAD_REGEX = "(?:(?:http[s]?://)?(?:\\w+\\.)?reddit\\.com/|/)?(r/[A-Za-z0-9_.]+/comments/\\w+/\\w+/\\w+/?.*)".toRegex()
val REDDIT_COMMENTS_REGEX = "(?:(?:http[s]?://)?(?:\\w+\\.)?reddit\\.com/|/)?(r/[A-Za-z0-9_.]+/comments/\\w+(?:/\\w+)?/?).*".toRegex()
val PREFIXED_REDDIT_ITEM = "/?[rRuU]/[^\\s]+".toRegex()

@Parcelize
enum class UrlType : Parcelable {
    IMAGE,
    GIF,
    GIFV,
    GFYCAT,
    REDGIFS,
    HLS,
    REDDIT_VIDEO,
    STANDARD_VIDEO,
    IMGUR_ALBUM,
    IMGUR_IMAGE,
    SUBREDDIT,
    USER,
    REDDIT_COMMENTS,
    REDDIT_THREAD,
    REDDIT_GALLERY,
    OTHER
}

fun String.getUrlType(): UrlType {
    return when {
        GIFV_REGEX.matches(this) -> UrlType.GIFV
        GIF_REGEX.matches(this) -> UrlType.GIF
        HLS_REGEX.matches(this) -> UrlType.HLS
        REDDIT_VIDEO_REGEX.matches(this) -> UrlType.REDDIT_VIDEO
        STANDARD_VIDEO_REGEX.matches(this) -> UrlType.STANDARD_VIDEO
        IMGUR_ALBUM_REGEX.matches(this) -> UrlType.IMGUR_ALBUM
        IMGUR_IMAGE_REGEX.matches(this) -> UrlType.IMGUR_IMAGE
        SUBREDDIT_REGEX.matches(this) -> UrlType.SUBREDDIT
        REDDIT_USER_REGEX.matches(this) -> UrlType.USER
        REDDIT_GALLERY_REGEX.matches(this) -> UrlType.REDDIT_GALLERY
        REDDIT_THREAD_REGEX.matches(this) -> UrlType.REDDIT_THREAD
        REDDIT_COMMENTS_REGEX.matches(this) -> UrlType.REDDIT_COMMENTS
        GFYCAT_REGEX.matches(this) -> UrlType.GFYCAT
        REDGIFS_REGEX.matches(this) -> UrlType.REDGIFS
        IMAGE_REGEX.containsMatchIn(this) -> UrlType.IMAGE
        else -> UrlType.OTHER
    }
}

fun String.getImgurHashFromUrl(): String? {
    return when (this.getUrlType()) {
        UrlType.IMGUR_ALBUM -> {
            IMGUR_ALBUM_REGEX.getFirstGroup(this)
        }
        UrlType.IMGUR_IMAGE -> {
            IMGUR_IMAGE_REGEX.getFirstGroup(this)
        }
        else -> null
    }
}

fun Regex.getFirstGroup(str: String): String? {
    return this.find(str)?.groups?.get(1)?.value
}

fun String.stripImageUrl() = IMAGE_REGEX.find(this)?.value

enum class MediaType {
    PICTURE,
    IMGUR_PICTURE,
    GIF,
    VIDEO,
    VIDEO_PREVIEW,
    GFYCAT,
    REDGIFS,
    IMGUR_ALBUM
}

enum class SimpleMediaType {
    PICTURE,
    VIDEO
}