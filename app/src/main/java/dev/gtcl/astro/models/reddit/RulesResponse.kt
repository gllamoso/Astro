package dev.gtcl.astro.models.reddit

import com.squareup.moshi.Json

data class RulesResponse(
    val rules: List<Rule>,
    @Json(name = "site_rules")
    val siteRules: List<String>)
data class Rule(
    val kind: RuleFor,
    @Json(name = "short_name") val shortName: String,
    val description: String
)

enum class RuleFor{
    @Json(name = "link")
    POST,
    @Json(name ="comment")
    COMMENT,
    @Json(name = "all")
    ALL
}