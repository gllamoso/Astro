package dev.gtcl.astro.models.reddit

import com.squareup.moshi.Json
import dev.gtcl.astro.html.ParsedHtmlSegment
import dev.gtcl.astro.html.parseToHtmlSegments

data class RulesResponse(
    val rules: List<Rule>,
    @Json(name = "site_rules")
    val siteRules: List<String>
)

data class Rule(
    val kind: RuleFor,
    @Json(name = "short_name")
    val shortName: String,
    val description: String,
    @Json(name = "description_html")
    val descriptionHtml: String
) {
    @Transient
    private var _parsedDescription: List<ParsedHtmlSegment>? = null

    fun parseDescription(): List<ParsedHtmlSegment> {
        if (_parsedDescription == null) {
            _parsedDescription = descriptionHtml.parseToHtmlSegments()
        }
        return _parsedDescription!!
    }
}

enum class RuleFor {
    @Json(name = "link")
    POST,

    @Json(name = "comment")
    COMMENT,

    @Json(name = "all")
    ALL
}