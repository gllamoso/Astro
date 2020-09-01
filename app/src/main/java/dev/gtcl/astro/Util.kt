package dev.gtcl.astro

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.text.util.Linkify
import android.util.Base64
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.Nullable
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonDataException
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.markdown.CustomMarkwonPlugin
import dev.gtcl.astro.models.reddit.listing.*
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import java.lang.reflect.Field
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.*
import kotlin.Exception

const val REDDIT_CLIENT_ID = "NjgsWrF6i2B0Jw"
const val REDDIT_REDIRECT_URL = "http://reddit.gtcl.com/redirect"
const val REDDIT_AUTH_URL = "https://www.reddit.com/api/v1/authorize.compact?client_id=%s&response_type=code&state=%s&redirect_uri=%s&duration=permanent&scope=account identity edit flair history modconfig modflair modlog modposts modwiki mysubreddits privatemessages read report save submit subscribe vote wikiedit wikiread"

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
    RISING,
    // USED FOR SEARCH
    @SerializedName("relevance")
    RELEVANCE,
    @SerializedName("comments")
    COMMENTS
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

enum class RuleType{
    RULE,
    SITE_RULE,
    OTHER
}

data class RuleData(
    val rule: String,
    val type: RuleType
)

enum class MediaType{
    PICTURE,
    IMGUR_PICTURE,
    GIF,
    VIDEO,
    GFYCAT,
    REDGIFS,
    IMGUR_ALBUM
}

enum class SimpleMediaType{
    PICTURE,
    VIDEO
}

const val SECONDS_IN_YEAR = 31_536_000.toLong()
const val SECONDS_IN_MONTH = 2_592_000.toLong()
const val SECONDS_IN_WEEK = 604_800.toLong()
const val SECONDS_IN_DAY = 86_400.toLong()
const val SECONDS_IN_HOUR = 3_600.toLong()
const val SECONDS_IN_MINUTE = 60.toLong()

fun timeSince(seconds: Long): String{
    val timeNow = System.currentTimeMillis()/1000 // Instant.now().epochTime for API 26
    var secondsAgo = timeNow - seconds

    val sb = StringBuilder()

    if(secondsAgo > SECONDS_IN_YEAR){
        val years = secondsAgo/SECONDS_IN_YEAR
        secondsAgo %= SECONDS_IN_YEAR
        sb.append("${years}y ")
    }

    when{
        secondsAgo > SECONDS_IN_MONTH -> {
            val months = secondsAgo/SECONDS_IN_MONTH
            sb.append("${months}mo")
        }
        secondsAgo > SECONDS_IN_WEEK -> {
            val weeks = secondsAgo/SECONDS_IN_WEEK
            sb.append("${weeks}w")
        }
        secondsAgo > SECONDS_IN_DAY -> {
            val days = secondsAgo/SECONDS_IN_DAY
            sb.append("${days}d")
        }
        secondsAgo > SECONDS_IN_HOUR -> {
            val hours = secondsAgo/SECONDS_IN_HOUR
            sb.append("${hours}h")
        }
        secondsAgo > SECONDS_IN_MINUTE -> {
            val minutes = secondsAgo/SECONDS_IN_MINUTE
            sb.append("${minutes}m")
        }
        else -> sb.append("1m")
    }

    return sb.toString()
}

fun numFormatted(num: Int): String{
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

fun getEncodedAuthString(): String{
    val authString = "$REDDIT_CLIENT_ID:"
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

fun String.toValidImgUrl(): String? = IMAGE_REGEX.find(this)?.value

operator fun <T> MutableLiveData<MutableList<T>>.plusAssign(values: List<T>) {
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

sealed class PostContent
class TextPost(val body: String): PostContent()
class ImagePost(val uri: Uri): PostContent()
class LinkPost(val url: URL): PostContent()

val IMGUR_GALLERY_REGEX = "http[s]?://(m.)?imgur\\.com/gallery/[A-Za-z0-9]+".toRegex()
val IMGUR_ALBUM_REGEX = "http[s]?://(m.)?imgur\\.com/a/[A-Za-z0-9]+".toRegex()
val IMGUR_IMAGE_REGEX = "http[s]?://(m.)?imgur\\.com/[A-Za-z0-9]+".toRegex()
val IMAGE_REGEX = "http[s]?://.+\\.(jpg|png|svg|jpeg)".toRegex()
val GIF_REGEX = "http[s]?://.+\\.gif".toRegex()
val GIFV_REGEX = "http[s]?://.+\\.gifv".toRegex()
val GFYCAT_REGEX =  "http[s]?://(www\\.)?gfycat.com/[\\-a-zA-Z0-9_]+".toRegex()
val REDGIFS_REGEX = "http[s]?://(www\\.)?redgifs.com/watch/\\w+".toRegex()
val HLS_REGEX = "http[s]?://.+/HLSPlaylist\\.m3u8.*".toRegex()
val REDDIT_VIDEO_REGEX = "http[s]?://v.redd.it/\\w+".toRegex()
val STANDARD_VIDEO = "http[s]?://.+\\.(mp4)".toRegex()
val REDDIT_COMMENTS_REGEX = "http[s]?://www\\.reddit\\.com/r/\\w+/comments/.+".toRegex()

@Parcelize
enum class UrlType: Parcelable {
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
    REDDIT_COMMENTS,
    OTHER
}

fun String.getUrlType(): UrlType?{
    return when{
        IMGUR_ALBUM_REGEX.matches(this) or IMGUR_GALLERY_REGEX.matches(this) -> UrlType.IMGUR_ALBUM
        IMGUR_URL_REGEX.matches(this) -> UrlType.IMGUR_IMAGE
        IMAGE_REGEX.matches(this) or IMGUR_IMAGE_REGEX.matches(this) -> UrlType.IMAGE
        GIF_REGEX.matches(this) -> UrlType.GIF
        GIFV_REGEX.matches(this) -> UrlType.GIFV
        GFYCAT_REGEX.matches(this) -> UrlType.GFYCAT
        REDGIFS_REGEX.matches(this) -> UrlType.REDGIFS
        HLS_REGEX.matches(this) -> UrlType.HLS
        REDDIT_VIDEO_REGEX.matches(this) -> UrlType.REDDIT_VIDEO
        STANDARD_VIDEO.matches(this) -> UrlType.STANDARD_VIDEO
        REDDIT_COMMENTS_REGEX.matches(this) -> UrlType.REDDIT_COMMENTS
        else -> UrlType.OTHER
    }
}

val IMGUR_URL_REGEX = "http[s]?://(m\\.)?imgur\\.com/\\w+".toRegex()
const val IMGUR_GALLERY_URL = "https://imgur.com/gallery/"
const val IMGUR_ALBUM_URL = "https://imgur.com/a/"
const val IMGUR_URL = "https://imgur.com/"

fun String.getImgurHashFromUrl(): String?{
    return when(this.getUrlType()){
        UrlType.IMGUR_ALBUM -> this.replace(IMGUR_ALBUM_URL, "").replace(IMGUR_GALLERY_URL, "")
        UrlType.IMGUR_IMAGE -> this.replace(IMGUR_URL, "")
        else -> null
    }
}

@ColorInt
fun Context.resolveColorAttr(@AttrRes colorAttr: Int): Int {
    val resolvedAttr = resolveThemeAttr(colorAttr)
    // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
    val colorRes = if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
    return ContextCompat.getColor(this, colorRes)
}

fun Context.resolveThemeAttr(@AttrRes attrRes: Int): TypedValue {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue
}

fun Exception.getErrorMessage(context: Context): String{
    val errorId = when(this){
        is SocketTimeoutException -> R.string.socket_timeout_error
        is UnknownHostException -> R.string.unknown_host_exception
        is NotLoggedInException -> R.string.user_must_be_logged_in
        is JsonDataException -> R.string.error_in_json_parsing
        else -> R.string.something_went_wrong
    }
    return context.getString(errorId)
}

enum class LeftDrawerHeader{
    HOME,
    MY_ACCOUNT,
    INBOX,
    SETTINGS
}

fun saveAccountToPreferences(context: Context, account: SavedAccount?){
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
    with(sharedPrefs.edit()) {
        val json = Gson().toJson(account)
        putString(CURRENT_USER_KEY, json)
        commit()
    }
}

fun rotateView(view: View, rotate: Boolean){
    view.animate().rotation(if(rotate){
        180F
    } else {
        0F
    })
}


fun createMarkwonInstance(context: Context, handleLink: (String) -> Unit): Markwon{
    return Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(CustomMarkwonPlugin(handleLink))
        .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
        .usePlugin(MovementMethodPlugin.create(BetterLinkMovementMethod.getInstance()))
        .build()
}

fun getListingTitle(context: Context, listing: Listing): String {
    return when (listing) {
        is FrontPage -> context.getString(R.string.frontpage)
        is All -> context.getString(R.string.all)
        is Popular -> context.getString(R.string.popular_tab_label)
        is SearchListing -> String.format(context.getString(R.string.search_title), listing.query)
        is MultiRedditListing -> listing.multiReddit.displayName
        is SubredditListing -> listing.displayName
        is SubscriptionListing -> listing.subscription.displayName
        is ProfileListing -> {
            context.getString(
                when (listing.info) {
                    ProfileInfo.OVERVIEW -> R.string.overview
                    ProfileInfo.SUBMITTED -> R.string.submitted
                    ProfileInfo.COMMENTS -> R.string.comments
                    ProfileInfo.UPVOTED -> R.string.upvoted
                    ProfileInfo.DOWNVOTED -> R.string.downvoted
                    ProfileInfo.GILDED -> R.string.gilded
                    ProfileInfo.HIDDEN -> R.string.hidden
                    ProfileInfo.SAVED -> R.string.saved
                }
            )
        }
    }
}

@Nullable
@Throws(
    IllegalAccessException::class,
    NoSuchFieldException::class
)
fun getMenuItemView(toolbar: Toolbar?, @IdRes menuItemId: Int): View? {
    val mMenuView: Field = Toolbar::class.java.getDeclaredField("mMenuView")
    mMenuView.isAccessible = true
    val menuView: Any? = mMenuView.get(toolbar)
    (menuView as ViewGroup).children.forEach {
        if(it.id == menuItemId) {
            return it
        }
    }
    return null
}

fun PopupWindow.showAsDropdown(anchor: View, content: View, width: Int, height: Int){
    this.apply {
        contentView = content
        isFocusable = true
        setWidth(width)
        setHeight(height)
        elevation = 20F
        showAsDropDown(anchor)
    }
}