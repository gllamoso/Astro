package dev.gtcl.astro.models.reddit

import com.squareup.moshi.Json

data class TrendingSubredditsResponse(
    @Json(name = "subreddit_names")
    val subredditNames: List<String>
)