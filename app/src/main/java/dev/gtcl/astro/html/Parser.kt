package dev.gtcl.astro.html

import dev.gtcl.astro.html.spans.*
import java.util.*

fun String.parseToHtmlSegments(): List<ParsedHtmlSegment> {
    val html = this.replace("&lt;".toRegex(), "<")
        .replace("&gt;".toRegex(), ">")
        .replace("&quot;".toRegex(), "\"")
        .replace("&#x200B;".toRegex(), "")
        .replace("&#32;".toRegex(), " ")
        .replace("&amp;".toRegex(), "&")
        .replace("<!-- SC_OFF -->".toRegex(), "")
        .replace("<!-- SC_ON -->".toRegex(), "")
        .replace("(?<!\\\\)\\\\n".toRegex(), "\n")

    val parsedSegments = mutableListOf<ParsedHtmlSegment>()
    val parsedText = StringBuilder()
    val spanPlaceholders = mutableListOf<SpanPlaceholder>()
    val quoteStack = Stack<QuoteStart>()

    var start = html.findNext('<', 0)
    var end = html.findNext('>', start)

    while (start in html.indices && end in html.indices) {
        when (html.substring(start + 1, end)) {
            "table" -> {
                val textSoFar = createParsedHtmlText(parsedText, spanPlaceholders, quoteStack)
                if (textSoFar.text.isNotEmpty()) {
                    parsedSegments.add(textSoFar)
                }

                val tableStart = end + 1
                val tableEnd = html.findNext("</table>".toRegex(), tableStart)
                parsedSegments.add(parseTable(html.substring(tableStart, tableEnd)))
                start = html.findNext('<', tableEnd + 1)
            }
            "hr/" -> {
                val textSoFar = createParsedHtmlText(parsedText, spanPlaceholders, quoteStack)
                if (textSoFar.text.isNotEmpty()) {
                    parsedSegments.add(textSoFar)
                }
                parsedSegments.add(HorizontalLine)
                start = html.findNext('<', end + 1)
            }
            "p" -> {
                if (parsedText.isNotEmpty()) { // Add spacing
                    parsedText.append("\n\n")
                    spanPlaceholders.add(SpanPlaceholder(parsedText.length - 1, parsedText.length, Spacing))
                }

                val paragraphStart = end + 1
                val paragraphEnd = html.findNext("</p>".toRegex(), paragraphStart)
                val paragraph = html.substring(paragraphStart, paragraphEnd)
                if (paragraph.startsWith("<code>\n") && paragraph.endsWith("\n</code>")) { // Check if codeblock
                    val textSoFar = createParsedHtmlText(parsedText, spanPlaceholders, quoteStack)
                    if (textSoFar.text.isNotEmpty()) {
                        parsedSegments.add(textSoFar)
                    }

                    val codeBlock = parseCodeBlock(paragraph)
                    parsedSegments.add(codeBlock)
                } else {
                    parseLine(
                        paragraph,
                        parsedText,
                        spanPlaceholders
                    )
                }

                start = html.findNext('<', paragraphEnd + 1)
            }
            "ol" -> {
                if (parsedText.isNotEmpty()) { // Add spacing
                    parsedText.append("\n\n")
                    spanPlaceholders.add(SpanPlaceholder(parsedText.length - 1, parsedText.length, Spacing))
                }

                var olUnclosed = 1
                var nextOlTag = html.findNext("</?ol>".toRegex(), end + 1)
                while (nextOlTag in end until length) {
                    val closingBracket = html.findNext('>', nextOlTag)
                    when (html.substring(nextOlTag + 1, closingBracket)) {
                        "ol" -> olUnclosed++
                        "/ol" -> {
                            olUnclosed--
                            if (olUnclosed == 0) {
                                parseList(
                                    html.substring(start, closingBracket + 1),
                                    parsedText,
                                    spanPlaceholders
                                )
                                start = html.findNext('<', closingBracket)
                                break
                            }
                        }
                    }
                    nextOlTag = html.findNext("</?ol>".toRegex(), closingBracket + 1)
                }
            }
            "ul" -> {
                if (parsedText.isNotEmpty()) { // Add spacing
                    parsedText.append("\n\n")
                    spanPlaceholders.add(SpanPlaceholder(parsedText.length - 1, parsedText.length, Spacing))
                }

                var ulUnclosed = 1
                var nextUlTag = html.findNext("</?ul>".toRegex(), end + 1)
                while (nextUlTag in end..html.length) {
                    val closingBracket = html.findNext('>', nextUlTag)
                    when (html.substring(nextUlTag + 1, closingBracket)) {
                        "ul" -> ulUnclosed++
                        "/ul" -> {
                            ulUnclosed--
                            if (ulUnclosed == 0) {
                                parseList(
                                    html.substring(start, closingBracket + 1),
                                    parsedText,
                                    spanPlaceholders
                                )
                                start = html.findNext('<', closingBracket)
                                break
                            }
                        }
                    }
                    nextUlTag = html.findNext("</?ul>".toRegex(), closingBracket + 1)
                }
            }
            "blockquote" -> {
                val startQuote = parsedText.length + if (parsedText.isNotEmpty()) 2 else 0
                quoteStack.add(QuoteStart(startQuote, spanPlaceholders.size))
                start = html.findNext('<', end + 1)
            }
            "/blockquote" -> {
                val quoteStart = quoteStack.pop()
                if(parsedText.isNotEmpty()){
                    spanPlaceholders.add(
                            quoteStart.placeholderPosition,
                            SpanPlaceholder(
                                    quoteStart.start,
                                    parsedText.length,
                                    Quote
                            )
                    )
                }
                start = html.findNext('<', end + 1)
            }
            "pre" -> { // Code block
                val textSoFar = createParsedHtmlText(parsedText, spanPlaceholders, quoteStack)
                if (textSoFar.text.isNotEmpty()) {
                    parsedSegments.add(textSoFar)
                }

                val preEnd = html.findNext(
                    "(?<=</pre>).".toRegex(RegexOption.DOT_MATCHES_ALL),
                    end + 1
                )

                val codeBlock = parseCodeBlock(html.substring(end, preEnd))
                parsedSegments.add(codeBlock)
                start = html.findNext('<', preEnd)
            }
            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                if (parsedText.isNotEmpty()) { // Add spacing
                    parsedText.append("\n\n")
                    spanPlaceholders.add(SpanPlaceholder(parsedText.length - 1, parsedText.length, Spacing))
                }

                val size = 7 - Integer.parseInt(html.substring(start + 2, end))

                val sbStartIndex = parsedText.length
                val placeholderIndex = spanPlaceholders.size
                val headingStart = end + 1
                val headingEnd = html.findNext("(?=</h\\d>)".toRegex(), start + 1)

                parseLine(
                    html.substring(headingStart, headingEnd),
                    parsedText,
                    spanPlaceholders
                )
                spanPlaceholders.add(
                    placeholderIndex,
                    SpanPlaceholder(
                        sbStartIndex,
                        parsedText.length,
                        Heading(size)
                    )
                )
                start = html.findNext('<', headingEnd)
            }
            else -> {
                start = html.findNext('<', end)
            }
        }
        if (start !in html.indices) break
        end = html.findNext('>', start + 1)
    }


    val lastTextSegment = createParsedHtmlText(parsedText, spanPlaceholders, quoteStack)
    if (lastTextSegment.text.isNotEmpty()) {
        parsedSegments.add(lastTextSegment)
    }

    return parsedSegments
}

fun parseLine(html: String, sb: StringBuilder, spanPlaceholders: MutableList<SpanPlaceholder>) {
    val htmlTrimmed = html
        .trimEnd()

    var i = 0
    var boldStart = sb.length
    var italicStart = sb.length
    var superScriptStart = sb.length
    var inlineCodeStart = sb.length
    var strikeThroughStart = sb.length
    var spoilerStart = sb.length
    var hyperlinkStart = sb.length
    var hyperLink = Hyperlink("")

    while (i in htmlTrimmed.indices) {
        val currentChar = htmlTrimmed[i]
        if (currentChar == '<') { // start of tag
            val closingBracket = htmlTrimmed.findNext('>', i)
            val tag = htmlTrimmed.substring(i + 1, closingBracket)
            when {
                tag.startsWith("span") -> {
                    spoilerStart = sb.length
                }
                tag.startsWith("a ") -> {
                    val linkStart = htmlTrimmed.findNext("(?<=\").".toRegex(), i + 1)
                    val linkEnd = htmlTrimmed.findNext("\"".toRegex(), linkStart + 1)
                    hyperLink = Hyperlink(
                        htmlTrimmed.substring(linkStart, linkEnd)
                            .replace("&lt;".toRegex(), "<")
                            .replace("&gt;".toRegex(), ">")
                            .replace("&quot;".toRegex(), "\"")
                            .replace("&amp;".toRegex(), "&")
                    )
                    hyperlinkStart = sb.length
                }
                tag.startsWith("/") -> {
                    when (tag) {
                        "/strong" -> spanPlaceholders.add(
                            SpanPlaceholder(
                                boldStart,
                                sb.length,
                                Bold
                            )
                        )
                        "/em" -> spanPlaceholders.add(
                            SpanPlaceholder(
                                italicStart,
                                sb.length,
                                Italicize
                            )
                        )
                        "/sup" -> spanPlaceholders.add(
                            SpanPlaceholder(
                                superScriptStart,
                                sb.length,
                                Superscript
                            )
                        )
                        "/code" -> spanPlaceholders.add(
                            SpanPlaceholder(
                                inlineCodeStart,
                                sb.length,
                                InlineCode
                            )
                        )
                        "/del" -> spanPlaceholders.add(
                            SpanPlaceholder(
                                strikeThroughStart,
                                sb.length,
                                Strikethrough
                            )
                        )
                        "/span" -> spanPlaceholders.add(
                            SpanPlaceholder(
                                spoilerStart,
                                sb.length,
                                Spoiler
                            )
                        )
                        "/a" -> spanPlaceholders.add(
                            SpanPlaceholder(
                                hyperlinkStart,
                                sb.length,
                                hyperLink
                            )
                        )
                    }
                }
                else -> { // tag is possibly recognized
                    when (tag) {
                        "strong" -> boldStart = sb.length
                        "em" -> italicStart = sb.length
                        "sup" -> superScriptStart = sb.length
                        "code" -> inlineCodeStart = sb.length
                        "del" -> strikeThroughStart = sb.length
                    }
                }
            }

            i = closingBracket + 1
        } else { // start of text
            if (currentChar == '&') {
                val nextSemiColonIndex = htmlTrimmed.findNext(';', i + 1)
                if (nextSemiColonIndex in htmlTrimmed.indices) {
                    when (htmlTrimmed.substring(i, nextSemiColonIndex + 1)) {
                        "&lt;" -> {
                            sb.append("<")
                            i = nextSemiColonIndex + 1
                        }
                        "&gt;" -> {
                            sb.append(">")
                            i = nextSemiColonIndex + 1
                        }
                        "&quot;" -> {
                            sb.append("\"")
                            i = nextSemiColonIndex + 1
                        }
                        "&#x200B;" -> {
                            i = nextSemiColonIndex + 1
                        }
                        "&#32;" -> {
                            sb.append(" ")
                            i = nextSemiColonIndex + 1
                        }
                        "&amp;" -> {
                            sb.append("&")
                            i = nextSemiColonIndex + 1
                        }
                        "&#37;" -> {
                            sb.append("%")
                            i = nextSemiColonIndex + 1
                        }
                        "&#39;" -> {
                            sb.append("'")
                            i = nextSemiColonIndex + 1
                        }
                        "&nbsp;" -> {
                            i = nextSemiColonIndex + 1
                        }
                        else -> {
                            sb.append(currentChar)
                            i++
                        }
                    }
                } else {
                    sb.append(currentChar)
                    i++
                }
            } else if (currentChar == '\n'){
                sb.append(' ')
                i++
            } else {
                sb.append(currentChar)
                i++
            }
        }
    }
}

fun parseTable(html: String): Table {
    // Get Header Cells
    val headerCells = mutableListOf<HeaderCell>()
    var start = html.findNext("<th[\\s>]".toRegex())
    var end = html.findNext(">".toRegex(), start + 1)
    while (start in html.indices && end in html.indices) {
        val substring = html.substring(start, end)
        val alignment = when {
            substring.contains("right") -> Cell.Alignment.RIGHT
            substring.contains("center") -> Cell.Alignment.CENTER
            else -> Cell.Alignment.LEFT
        }
        val closingTagIndex = html.findNext("</th>".toRegex(), end + 1)
        val sb = StringBuilder()
        val spanPlaceholders = mutableListOf<SpanPlaceholder>()
        parseLine(html.substring(end + 1, closingTagIndex).trim(), sb, spanPlaceholders)
        val parsedHtml = SimpleText(
            sb.toString(),
            spanPlaceholders
        )
        headerCells.add(
            HeaderCell(
                parsedHtml,
                alignment
            )
        )
        start = html.findNext("<th[\\s>]".toRegex(), closingTagIndex + 1)
        end = html.findNext(">".toRegex(), start)
    }

    // Get Body Cells
    start = html.findNext("<td".toRegex())
    end = html.findNext(">".toRegex(), start)
    val allCells = mutableListOf<Cell>()
    while (start > 0 && end > 0 && start < html.length && end < html.length) {
        val substring = html.substring(start, end + 1)
        val alignment = when {
            substring.contains("right") -> Cell.Alignment.RIGHT
            substring.contains("center") -> Cell.Alignment.CENTER
            else -> Cell.Alignment.LEFT
        }
        val closingTagIndex = html.findNext("</td>".toRegex(), end)
        val sb = StringBuilder()
        val spanPlaceholders = mutableListOf<SpanPlaceholder>()
        parseLine(html.substring(end + 1, closingTagIndex).trim(), sb, spanPlaceholders)
        val parsedHtml = SimpleText(
            sb.toString(),
            spanPlaceholders
        )
        allCells.add(
            Cell(
                parsedHtml,
                alignment
            )
        )
        start = html.findNext("<td".toRegex(), closingTagIndex + 1)
        end = html.findNext(">".toRegex(), start)
    }

    val cols = headerCells.size
    val rows = allCells.size / cols
    val cellRows = mutableListOf<List<Cell>>()

    for (i in 0 until rows) {
        val currentRow = mutableListOf<Cell>()
        for (j in 0 until cols) {
            currentRow.add(allCells[(i * cols) + j])
        }
        cellRows.add(currentRow)
    }

    return Table(headerCells, cellRows)
}

fun parseList(
    html: String,
    sb: StringBuilder,
    spanPlaceholders: MutableList<SpanPlaceholder>
) {
    var depth = 0
    var bracketStart = 0
    var bracketEnd = html.findNext('>', bracketStart + 1)
    val itemsInEachDepth = arrayListOf<Int>()
    val depthOrdered = arrayListOf(true)
    var firstItemFound = false

    while (bracketStart in html.indices) {
        when (html.substring(bracketStart + 1, bracketEnd)) {
            "ol" -> {
                depth++
                while(depth > depthOrdered.lastIndex){
                    depthOrdered.add(true)
                }
            }
            "ul" -> {
                depth++
                while(depth > depthOrdered.lastIndex){
                    depthOrdered.add(false)
                }
            }
            "/ul", "/ol" -> {
                depth--
                for (i in depth + 1 until itemsInEachDepth.size) {
                    itemsInEachDepth.removeAt(i)
                }
                for (i in depth + 1 until depthOrdered.size){
                    depthOrdered.removeAt(i)
                }
            }
            "li" -> {
                while (itemsInEachDepth.size < (depth + 1)) {
                    itemsInEachDepth.add(0)
                }
                itemsInEachDepth[depth]++
                val itemStart = bracketEnd + 1
                val itemEnd = html.findNext("(?:\n\n|</li>)".toRegex(), itemStart + 1)
                if (firstItemFound) {
                    sb.append("\n")
                } else {
                    firstItemFound = true
                }
                val sbIndex = sb.length
                val spanPlaceholderIndex = spanPlaceholders.size
                parseLine(html.substring(itemStart, itemEnd), sb, spanPlaceholders)
                spanPlaceholders.add(
                    spanPlaceholderIndex,
                    SpanPlaceholder(
                        sbIndex,
                        sb.length,
                        if (depthOrdered[depth]) {
                            OrderedListItem(depth, itemsInEachDepth[depth])
                        } else {
                            UnorderedListItem(depth)
                        }
                    )
                )
            }
        }
        bracketStart = html.findNext("(?:</?ol>|</?ul>|<li>)".toRegex(), bracketEnd + 1)
        bracketEnd = html.findNext('>', bracketStart + 1)
    }
}

fun parseCodeBlock(codeBlock: String): CodeBlock {
    val regexOptions = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
    val codeBlockStart = codeBlock.findNext("(?<=<code>).".toRegex(regexOptions))
    val codeBlockEnd = codeBlock.findNext("</code>".toRegex(), codeBlockStart + 1)
    val codeSb = StringBuilder()
    val codeSpanPlaceholders = mutableListOf<SpanPlaceholder>()
    parseLine(
        codeBlock.substring(codeBlockStart, codeBlockEnd).trim(),
        codeSb,
        codeSpanPlaceholders
    )
    return CodeBlock(
        codeSb.toString(),
        codeSpanPlaceholders.toList()
    )
}

fun createParsedHtmlText(
    sb: StringBuilder,
    spanPlaceholders: MutableList<SpanPlaceholder>,
    quoteStack: Stack<QuoteStart>
): SimpleText {
    for ((start, placeholderPosition) in quoteStack) {
        if (start < sb.length) {
            spanPlaceholders.add(
                placeholderPosition,
                SpanPlaceholder(
                    start,
                    sb.length,
                    Quote
                )
            )
        }
    }

    val result = SimpleText(
        sb.toString(),
        spanPlaceholders.toList()
    )

    // Clear everything before adding table
    val unclosedQuotes = quoteStack.size
    quoteStack.clear()
    sb.clear()
    spanPlaceholders.clear()
    for (i in 0 until unclosedQuotes) {
        quoteStack.add(QuoteStart(0, 0))
    }

    return result
}

fun CharSequence.findNext(char: Char, start: Int = 0): Int {
    if (start >= length || start < 0) {
        return -1
    }

    var i = start
    while (i < length) {
        if (get(i++) == char) {
            return i - 1
        }
    }
    return -1
}

fun CharSequence.findNext(regex: Regex, start: Int = 0): Int {
    if (start >= length || start < 0) {
        return -1
    }

    val match = regex.find(this, start)
    if (match != null) {
        return match.range.first
    }
    return -1
}

data class QuoteStart(
    val start: Int,
    val placeholderPosition: Int
)