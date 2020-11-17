package dev.gtcl.astro.html

import dev.gtcl.astro.html.spans.SpanPlaceholder

sealed class ParsedHtmlSegment

data class SimpleText(
    val text: String,
    val spanPlaceholders: List<SpanPlaceholder>
) : ParsedHtmlSegment()

data class Table(
    val headers: List<HeaderCell>,
    val cellRows: List<List<Cell>>
) : ParsedHtmlSegment()

object HorizontalLine : ParsedHtmlSegment()

data class CodeBlock(
    val text: String,
    val spanPlaceholders: List<SpanPlaceholder>
) : ParsedHtmlSegment()