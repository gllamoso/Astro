package dev.gtcl.reddit

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import android.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.ui.fragments.subreddits.trending.TrendingSubredditPost
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.URL
import java.util.*

enum class PostSort{
    @SerializedName("best")
    BEST,
    @SerializedName("hot")
    HOT,
    @SerializedName("new")
    NEW,
    @SerializedName("top")
    TOP,
    @SerializedName("controversial")
    CONTROVERSIAL,
    @SerializedName("rising")
    RISING
}

enum class CommentSort{
    @SerializedName("best")
    BEST,
    @SerializedName("top")
    TOP,
    @SerializedName("new")
    NEW,
    @SerializedName("controversial")
    CONTROVERSIAL,
    @SerializedName("old")
    OLD,
    @SerializedName("random")
    RANDOM,
    @SerializedName("qa")
    QA
}

enum class Time{
    @SerializedName("hour")
    HOUR,
    @SerializedName("day")
    DAY,
    @SerializedName("week")
    WEEK,
    @SerializedName("month")
    MONTH,
    @SerializedName("year")
    YEAR,
    @SerializedName("all")
    ALL
}

enum class ProfileInfo {
    @SerializedName("overview")
    OVERVIEW,
    @SerializedName("submitted")
    SUBMITTED,
    @SerializedName("comments")
    COMMENTS,
    @SerializedName("upvoted")
    UPVOTED,
    @SerializedName("downvoted")
    DOWNVOTED,
    @SerializedName("hidden")
    HIDDEN,
    @SerializedName("saved")
    SAVED,
    @SerializedName("gilded")
    GILDED
}

enum class SubredditWhere{
    @SerializedName("popular")
    POPULAR,
    @SerializedName("new")
    NEW,
    @SerializedName("gold")
    GOLD,
    @SerializedName("default")
    DEFAULT
}

enum class SubredditMineWhere{
    @SerializedName("contributor")
    CONTRIBUTOR,
    @SerializedName("moderator")
    MODERATOR,
    @SerializedName("streams")
    STREAMS,
    @SerializedName("subscriber")
    SUBSCRIBER
}

enum class SubscribeAction{
    @SerializedName("sub")
    SUBSCRIBE,
    @SerializedName("unsub")
    UNSUBSCRIBE
}

enum class MessageWhere{
    @SerializedName("inbox")
    INBOX,
    @SerializedName("unread")
    UNREAD,
    @SerializedName("sent")
    SENT
}

enum class SubscriptionType{
    @SerializedName("multi")
    MULTIREDDIT,
    @SerializedName("sub")
    SUBREDDIT,
    @SerializedName("user")
    USER
}

enum class Visibility{
    @SerializedName("private")
    @Json(name = "private")
    PRIVATE,
    @SerializedName("public")
    @Json(name = "public")
    PUBLIC,
    @SerializedName("hidden")
    @Json(name = "hidden")
    HIDDEN
}

enum class MediaType{
    PICTURE,
    GIF,
    VIDEO,
    GFYCAT,
    IMGUR_ALBUM
}

const val SECONDS_IN_YEAR = 31_536_000.toLong()
const val SECONDS_IN_MONTH = 2_592_000.toLong()
const val SECONDS_IN_WEEK = 604_800.toLong()
const val SECONDS_IN_DAY = 86_400.toLong()
const val SECONDS_IN_HOUR = 3_600.toLong()
const val SECONDS_IN_MINUTE = 60.toLong()

fun timeSince(context: Context, seconds: Long): String{
    val timeNow = System.currentTimeMillis()/1000 // Instant.now().epochTime for API 26
    val secondsAgo = timeNow - seconds

    return when{
        secondsAgo > SECONDS_IN_YEAR -> {
            val years = secondsAgo/SECONDS_IN_YEAR
            if(years > 1) String.format(context.getString(R.string.years_ago), years)
            else context.getString(R.string.year_ago)
        }
        secondsAgo > SECONDS_IN_MONTH -> {
            val months = secondsAgo/SECONDS_IN_MONTH
            if(months > 1) String.format(context.getString(R.string.months_ago), months)
            else context.getString(R.string.month_ago)
        }
        secondsAgo > SECONDS_IN_WEEK -> {
            val weeks = secondsAgo/SECONDS_IN_WEEK
            if(weeks > 1) String.format(context.getString(R.string.weeks_ago), weeks)
            else context.getString(R.string.week_ago)
        }
        secondsAgo > SECONDS_IN_DAY -> {
            val days = secondsAgo/SECONDS_IN_DAY
            if(days > 1) String.format(context.getString(R.string.days_ago), days)
            else context.getString(R.string.day_ago)
        }
        secondsAgo > SECONDS_IN_HOUR -> {
            val hours = secondsAgo/SECONDS_IN_HOUR
            if(hours > 1) String.format(context.getString(R.string.hours_ago), hours)
            else context.getString(R.string.hour_ago)
        }
        secondsAgo > SECONDS_IN_MINUTE -> {
            val minutes = secondsAgo/SECONDS_IN_MINUTE
            if(minutes > 1) String.format(context.getString(R.string.minutes_ago), minutes)
            else context.getString(R.string.a_moment_ago)
        }
        else -> context.getString(R.string.a_moment_ago)
    }
}

fun numFormatted(num: Long): String{
    val sb = StringBuilder()
    when {
        num >= 1_000_000 -> {
            sb.append(num / 1_000_000)
            val decimal = num % 1_000_000 / 100_000
            if(decimal > 0){
                sb.append(".")
                sb.append(decimal)
            }
            sb.append("M")
        }
        num >= 1_000 -> {
            sb.append(num / 1_000)
            val decimal = num % 1_000 / 100
            if(decimal > 0){
                sb.append(".")
                sb.append(decimal)
            }
            sb.append("k")
        }
        else -> {
            sb.append(num)
        }
    }
    return sb.toString()
}

fun buildMediaSource(context: Context, uri: Uri): MediaSource {
    val userAgent = "exoplayer"

    return if (uri.lastPathSegment!!.contains("mp3") || uri.lastPathSegment!!.contains("mp4")) {
        ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
            .createMediaSource(uri)
    } else if (uri.lastPathSegment!!.contains("m3u8")) {
        HlsMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
            .createMediaSource(uri)
    } else {
//        val dataSourceFactory = DefaultDataSourceFactory(context, userAgent)
//        val mediaSourceFactory = DashMediaSource.Factory(dataSourceFactory)
//        mediaSourceFactory.createMediaSource(uri)
        val dataSourceFactory = DefaultDataSourceFactory(context, userAgent)
        val dashChunkSourceFactory = DefaultDashChunkSource.Factory(dataSourceFactory)

        DashMediaSource.Factory(dashChunkSourceFactory, dataSourceFactory)
            .createMediaSource(uri)
    }
}

fun getEncodedAuthString(context: Context): String{
    val clientID = context.getText(R.string.client_id)
    val authString = "$clientID:"
    return Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
}

enum class Vote(val value: Int){
    UPVOTE(1),
    DOWNVOTE(-1),
    UNVOTE(0)
}

suspend fun setItemsReadStatus(items: List<Item>, readIds: HashSet<String>){
    withContext(Dispatchers.Default){
        if(readIds.isEmpty()){
            return@withContext
        }
        for(item: Item in items){
            if(item is Post){
                item.isRead = readIds.contains(item.name)
            }
        }
    }
}

suspend fun setSubs(items: List<Item>, subscribedSubsHash: HashSet<String>){
    withContext(Dispatchers.Default){
        for(item: Item in items.filterIsInstance<Subreddit>()){
            (item as Subreddit).userSubscribed = subscribedSubsHash.contains(item.displayName)
        }
    }
}

suspend fun setSubsAndFavoritesInTrendingPost(items: List<TrendingSubredditPost>, subscribedSubsHash: HashSet<String>){
    withContext(Dispatchers.Default){
        for(item: TrendingSubredditPost in items){
            item.setSubscribedTo(subscribedSubsHash)
        }
    }
}

fun String.toValidImgUrl(): String? = "http.+\\.(png|jpg)".toRegex().find(this)?.value

operator fun <T> MutableLiveData<ArrayList<T>>.plusAssign(values: List<T>) {
    val value = this.value ?: arrayListOf()
    value.addAll(values)
    this.value = value
}

operator fun <T> MutableLiveData<MutableSet<T>>.plusAssign(item: T){
    val value = this.value ?: mutableSetOf()
    value.add(item)
    this.value = value
}

operator fun <T> MutableLiveData<MutableSet<T>>.minusAssign(item: T){
    val value = this.value ?: hashSetOf()
    value.remove(item)
    this.value = value
}

fun PopupMenu.forceIcons(){
    try {
        val fieldPopup = PopupMenu::class.java.getDeclaredField("mPopup")
        fieldPopup.isAccessible = true
        val mPopup = fieldPopup.get(this)
        mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java)
            .invoke(mPopup, true)
    } catch(e: Exception){
        Log.e("Popup", "Error showing menu icons.", e)
    }
}

sealed class PostContent(val postType: PostType)
class TextPost(val body: String): PostContent(PostType.TEXT)
class ImagePost(val uri: Uri): PostContent(PostType.IMAGE)
class LinkPost(val url: URL): PostContent(PostType.URL)

val IMGUR_GALLERY_REGEX = "http[s]?://imgur\\.com/gallery/\\w+".toRegex()
val IMGUR_ALBUM_REGEX = "http[s]?://imgur\\.com/a/\\w+".toRegex()
val IMAGE_REGEX = "https[s]?://.+\\.(jpg|png|svg)".toRegex()
val GIF_REGEX = "https[s]?://.+\\.gif".toRegex()
val GIFV_REGEX = "https[s]?://.+\\.gifv".toRegex()
val GFYCAT_REGEX =  "https[s]?://(www\\.)?gfycat.com/\\w+".toRegex()
val HLS_REGEX = "https[s]?://.+/HLSPlaylist\\.m3u8.*".toRegex()
val REDDIT_VIDEO_REGEX = "https[s]?://v.redd.it/\\w+".toRegex()
val STANDARD_VIDEO = "https[s]?://.+\\.(mp4)".toRegex()
val REDDIT_COMMENTS_REGEX = "http[s]?://www\\.reddit\\.com/r/\\w+/comments/.+".toRegex()

@Parcelize
enum class UrlType: Parcelable {
    IMAGE,
    GIF,
    GIFV,
    GFYCAT,
    HLS,
    REDDIT_VIDEO,
    STANDARD_VIDEO,
    IMGUR_ALBUM,
    REDDIT_COMMENTS,
    OTHER
}


fun String.getUrlType(): UrlType{
    return when{
        IMGUR_ALBUM_REGEX.matches(this) or IMGUR_GALLERY_REGEX.matches(this) -> UrlType.IMGUR_ALBUM
        IMAGE_REGEX.matches(this) -> UrlType.IMAGE
        GIF_REGEX.matches(this) -> UrlType.GIF
        GIFV_REGEX.matches(this) -> UrlType.GIFV
        GFYCAT_REGEX.matches(this) -> UrlType.GFYCAT
        HLS_REGEX.matches(this) -> UrlType.HLS
        REDDIT_VIDEO_REGEX.matches(this) -> UrlType.REDDIT_VIDEO
        STANDARD_VIDEO.matches(this) -> UrlType.STANDARD_VIDEO
        REDDIT_COMMENTS_REGEX.matches(this) -> UrlType.REDDIT_COMMENTS
        else -> UrlType.OTHER
    }
}