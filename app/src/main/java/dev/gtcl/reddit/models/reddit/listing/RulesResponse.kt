package dev.gtcl.reddit.models.reddit.listing

import com.squareup.moshi.Json

data class RulesResponse(val rules: List<Rule>)
data class Rule(
    @Json(name = "short_name") val shortName: String,
    val description: String
)