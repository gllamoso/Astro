package dev.gtcl.reddit

import android.content.Context
import java.util.*

// TODO: Enums for post sorting
enum class PostSort(val stringValue: String){
    HOT("hot"),
    NEW("new"),
    TOP("top"),
    CONTROVERSIAL("controversial"),
    RISING("rising")
}

enum class CommentSort(val stringValue: String){
    BEST("best"),
    TOP("top"),
    NEW("new"),
    CONTROVERSIAL("controversial"),
    OLD("old"),
    RANDOM("random"),
    QA("qa")
}

enum class Time(val stringValue: String){
    HOUR("hour"),
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
    ALL("all")
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
            val years = seconds/SECONDS_IN_YEAR
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