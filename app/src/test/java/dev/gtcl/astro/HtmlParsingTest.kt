package dev.gtcl.astro

import dev.gtcl.astro.html.CodeBlock
import dev.gtcl.astro.html.SimpleText
import dev.gtcl.astro.html.Table
import dev.gtcl.astro.html.parseToHtmlSegments
import dev.gtcl.astro.html.spans.*
import org.junit.Assert.assertEquals
import org.junit.Test

class HtmlParsingTest {
    @Test
    fun simpleText(){
        val html = "&lt;!-- SC_OFF --&gt;&lt;div class=\"md\"&gt;&lt;p&gt;&lt;strong&gt;Bold&lt;/strong&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;em&gt;Italicize&lt;/em&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;a href=\"https://www.google.com\"&gt;Link&lt;/a&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;del&gt;Strike&lt;/del&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;code&gt;Inline Code&lt;/code&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;sup&gt;Super Script&lt;/sup&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;span class=\"md-spoiler-text\"&gt;Spoiler&lt;/span&gt;&lt;/p&gt;\\n\\n&lt;h1&gt;Heading&lt;/h1&gt;\\n\\n&lt;blockquote&gt;\\n&lt;p&gt;This is a quote&lt;/p&gt;\\n&lt;/blockquote&gt;\\n\\n&lt;ul&gt;\\n&lt;li&gt;Unordered item 1\\n\\n&lt;ul&gt;\\n&lt;li&gt;Unordered sub item 1&lt;/li&gt;\\n&lt;/ul&gt;&lt;/li&gt;\\n&lt;li&gt;Unordered item 2&lt;/li&gt;\\n&lt;/ul&gt;\\n\\n&lt;ol&gt;\\n&lt;li&gt;Ordered item 1\\n\\n&lt;ol&gt;\\n&lt;li&gt;Ordered sub item 1&lt;/li&gt;\\n&lt;/ol&gt;&lt;/li&gt;\\n&lt;li&gt;Ordered item 2&lt;/li&gt;\\n&lt;/ol&gt;\\n&lt;/div&gt;&lt;!-- SC_ON --&gt;"
        val segments = html.parseToHtmlSegments()
        assert(segments.size == 1 && segments[0] is SimpleText)

        val simpleText = segments[0] as SimpleText
        assert(simpleText.text == "Bold\n" +
                "\n" +
                "Italicize\n" +
                "\n" +
                "Link\n" +
                "\n" +
                "Strike\n" +
                "\n" +
                "Inline Code\n" +
                "\n" +
                "Super Script\n" +
                "\n" +
                "Spoiler\n" +
                "\n" +
                "Heading\n" +
                "\n" +
                "This is a quote\n" +
                "\n" +
                "Unordered item 1\n" +
                "Unordered sub item 1\n" +
                "Unordered item 2\n" +
                "\n" +
                "Ordered item 1\n" +
                "Ordered sub item 1\n" +
                "Ordered item 2")

        val placeholders = simpleText.spanPlaceholders
        assertEquals(placeholders[0], SpanPlaceholder(start = 0, end = 4, textModifier = Bold))
        assertEquals(placeholders[1], SpanPlaceholder(start = 5, end = 6, textModifier = Spacing))
        assertEquals(placeholders[2], SpanPlaceholder(start = 6, end = 15, textModifier = Italicize))
        assertEquals(placeholders[3], SpanPlaceholder(start = 16, end = 17, textModifier = Spacing))
        assertEquals(placeholders[4], SpanPlaceholder(start = 17, end = 21, textModifier = Hyperlink("https://www.google.com")))
        assertEquals(placeholders[5], SpanPlaceholder(start = 22, end = 23, textModifier = Spacing))
        assertEquals(placeholders[6], SpanPlaceholder(start = 23, end = 29, textModifier = Strikethrough))
        assertEquals(placeholders[7], SpanPlaceholder(start = 30, end = 31, textModifier = Spacing))
        assertEquals(placeholders[8], SpanPlaceholder(start = 31, end = 42, textModifier = InlineCode))
        assertEquals(placeholders[9], SpanPlaceholder(start = 43, end = 44, textModifier = Spacing))
        assertEquals(placeholders[10], SpanPlaceholder(start = 44, end = 56, textModifier = Superscript))
        assertEquals(placeholders[11], SpanPlaceholder(start = 57, end = 58, textModifier = Spacing))
        assertEquals(placeholders[12], SpanPlaceholder(start = 58, end = 65, textModifier = Spoiler))
        assertEquals(placeholders[13], SpanPlaceholder(start = 66, end = 67, textModifier = Spacing))
        assertEquals(placeholders[14], SpanPlaceholder(start = 67, end = 74, textModifier = Heading(6)))
        assertEquals(placeholders[15], SpanPlaceholder(start = 76, end = 91, textModifier = Quote))
        assertEquals(placeholders[16], SpanPlaceholder(start = 75, end = 76, textModifier = Spacing))
        assertEquals(placeholders[17], SpanPlaceholder(start = 92, end = 93, textModifier = Spacing))
        assertEquals(placeholders[18], SpanPlaceholder(start = 93, end = 109, textModifier = UnorderedListItem(1)))
        assertEquals(placeholders[19], SpanPlaceholder(start = 110, end = 130, textModifier = UnorderedListItem(2)))
        assertEquals(placeholders[20], SpanPlaceholder(start = 131, end = 147, textModifier = UnorderedListItem(1)))
        assertEquals(placeholders[21], SpanPlaceholder(start = 148, end = 149, textModifier = Spacing))
        assertEquals(placeholders[22], SpanPlaceholder(start = 149, end = 163, textModifier = OrderedListItem(1, 1)))
        assertEquals(placeholders[23], SpanPlaceholder(start = 164, end = 182, textModifier = OrderedListItem(2, 1)))
        assertEquals(placeholders[24], SpanPlaceholder(start = 183, end = 197, textModifier = OrderedListItem(1, 2)))
    }

    @Test
    fun table(){
        val html = "&lt;div class=\"md\"&gt;&lt;p&gt;&amp;#x200B;&lt;/p&gt;\\n\\n&lt;table&gt;&lt;thead&gt;\\n&lt;tr&gt;\\n&lt;th align=\"left\"&gt;Row 1, Col 1&lt;/th&gt;\\n&lt;th align=\"left\"&gt;Row 1, Col 2&lt;/th&gt;\\n&lt;th align=\"left\"&gt;Row 1, Col 3&lt;/th&gt;\\n&lt;/tr&gt;\\n&lt;/thead&gt;&lt;tbody&gt;\\n&lt;tr&gt;\\n&lt;td align=\"left\"&gt;Row 2, Col 1&lt;/td&gt;\\n&lt;td align=\"left\"&gt;Row 2, Col 2&lt;/td&gt;\\n&lt;td align=\"left\"&gt;Row 2, Col 3&lt;/td&gt;\\n&lt;/tr&gt;\\n&lt;/tbody&gt;&lt;/table&gt;\\n&lt;/div&gt;"
        val segments = html.parseToHtmlSegments()
        assert(segments.size == 1 && segments[0] is Table)
        val table = segments[0] as Table

        // Verify Headers
        val headers = table.headers
        assertEquals(headers.size, 3)
        assertEquals(headers[0].simpleText.text, "Row 1, Col 1")
        assertEquals(headers[1].simpleText.text, "Row 1, Col 2")
        assertEquals(headers[2].simpleText.text, "Row 1, Col 3")

        // Verify Rows
        assertEquals(table.cellRows.size, 1)
        val row1 = table.cellRows[0]
        assertEquals(row1.size, 3)
        assertEquals(row1[0].simpleText.text, "Row 2, Col 1")
        assertEquals(row1[1].simpleText.text, "Row 2, Col 2")
        assertEquals(row1[2].simpleText.text, "Row 2, Col 3")
    }

    @Test
    fun codeBlock(){
        val html = "&lt;div class=\"md\"&gt;&lt;pre&gt;&lt;code&gt;println(&amp;quot;Hello, world!&amp;quot;)\\n&lt;/code&gt;&lt;/pre&gt;\\n&lt;/div&gt;"
        val segments = html.parseToHtmlSegments()
        assert(segments.size == 1 && segments[0] is CodeBlock)
        val codeBlock = segments[0] as CodeBlock
        assertEquals(codeBlock.text, "println(\"Hello, world!\")")
    }

}