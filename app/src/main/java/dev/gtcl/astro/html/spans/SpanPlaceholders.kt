package dev.gtcl.astro.html.spans

sealed class TextModifier

object Bold : TextModifier()
object Italicize : TextModifier()
object Strikethrough : TextModifier()
object Spoiler : TextModifier()
object InlineCode : TextModifier()
data class Hyperlink(
    val link: String
) : TextModifier()

object Superscript : TextModifier()
object Spacing : TextModifier()

object Quote : TextModifier()
data class Heading(val size: Int) : TextModifier()
data class UnorderedListItem(
    val depth: Int
) : TextModifier()

data class OrderedListItem(
    val depth: Int,
    val value: Int
) : TextModifier()

data class SpanPlaceholder(
    val start: Int,
    val end: Int,
    val textModifier: TextModifier
)