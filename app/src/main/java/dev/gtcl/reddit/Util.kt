package dev.gtcl.reddit

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.annotation.MainThread
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import dev.gtcl.reddit.network.RedditApi
import java.util.*

enum class PostSort{
    best,
    hot,
    new,
    top,
    controversial,
    rising
}

enum class CommentSort{
    best,
    top,
    new,
    controversial,
    old,
    random,
    qa
}

enum class Time{
    hour,
    day,
    week,
    month,
    year,
    all
}

val STATE by lazy {
    UUID.randomUUID().toString()
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

fun getEncodedAuthString(context: Context): String{
    val clientID = context.getText(R.string.client_id)
    val authString = "$clientID:"
    return Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
}

fun buildMediaSource(context: Context, uri: Uri): MediaSource {
    val userAgent = "exoplayer-codelab"

    return if (uri.lastPathSegment!!.contains("mp3") || uri.lastPathSegment!!.contains("mp4")) {
        ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
            .createMediaSource(uri)
    } else if (uri.lastPathSegment!!.contains("m3u8")) {
        HlsMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
            .createMediaSource(uri)
    } else {
        val dataSourceFactory = DefaultDataSourceFactory(context, "exoplayer-codelab")
        val mediaSourceFactor = DashMediaSource.Factory(dataSourceFactory)
        mediaSourceFactor.createMediaSource(uri)
    }
}