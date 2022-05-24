package dev.gtcl.astro.models.reddit

data class RuleData(
    val rule: String,
    val type: RuleType
)

enum class RuleType {
    RULE,
    SITE_RULE,
    OTHER
}