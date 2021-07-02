package dev.gtcl.astro

import dev.gtcl.astro.html.CodeBlock
import dev.gtcl.astro.html.SimpleText
import dev.gtcl.astro.html.Table
import dev.gtcl.astro.html.parseToHtmlSegments
import dev.gtcl.astro.html.spans.*
import org.junit.Test

class HtmlParsingTest {
    @Test
    fun simpleText(){
        val html = "&lt;!-- SC_OFF --&gt;&lt;div class=\"md\"&gt;&lt;p&gt;&lt;strong&gt;Bold&lt;/strong&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;em&gt;Italicize&lt;/em&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;a href=\"https://www.google.com\"&gt;Link&lt;/a&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;del&gt;Strike&lt;/del&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;code&gt;Inline Code&lt;/code&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;sup&gt;Super Script&lt;/sup&gt;&lt;/p&gt;\\n\\n&lt;p&gt;&lt;span class=\"md-spoiler-text\"&gt;Spoiler&lt;/span&gt;&lt;/p&gt;\\n\\n&lt;h1&gt;Heading&lt;/h1&gt;\\n\\n&lt;blockquote&gt;\\n&lt;p&gt;This is a quote&lt;/p&gt;\\n&lt;/blockquote&gt;\\n\\n&lt;ul&gt;\\n&lt;li&gt;Unordered item 1\\n\\n&lt;ul&gt;\\n&lt;li&gt;Unordered sub item 1&lt;/li&gt;\\n&lt;/ul&gt;&lt;/li&gt;\\n&lt;li&gt;Unordered item 2&lt;/li&gt;\\n&lt;/ul&gt;\\n\\n&lt;ol&gt;\\n&lt;li&gt;Ordered item 1\\n\\n&lt;ol&gt;\\n&lt;li&gt;Ordered sub item 1&lt;/li&gt;\\n&lt;/ol&gt;&lt;/li&gt;\\n&lt;li&gt;Ordered item 2&lt;/li&gt;\\n&lt;/ol&gt;\\n&lt;/div&gt;&lt;!-- SC_ON --&gt;"
        val segments = html.parseToHtmlSegments()
        assert(segments.size == 1 && segments[0] is SimpleText)

        val simpleText = segments[0] as SimpleText
        println(simpleText.text)
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
        assert(placeholders[0].start == 0 && placeholders[0].end == 4 && placeholders[0].textModifier == Bold)
        assert(placeholders[1].start == 5 && placeholders[1].end == 6 && placeholders[1].textModifier == Spacing)
        assert(placeholders[2].start == 6 && placeholders[2].end == 15 && placeholders[2].textModifier == Italicize)
        assert(placeholders[3].start == 16 && placeholders[3].end == 17 && placeholders[3].textModifier == Spacing)
        assert(placeholders[4].start == 17 && placeholders[4].end == 21 && placeholders[4].textModifier == Hyperlink("https://www.google.com"))
        assert(placeholders[5].start == 22 && placeholders[5].end == 23 && placeholders[5].textModifier == Spacing)
        assert(placeholders[6].start == 23 && placeholders[6].end == 29 && placeholders[6].textModifier == Strikethrough)
        assert(placeholders[7].start == 30 && placeholders[7].end == 31 && placeholders[7].textModifier == Spacing)
        assert(placeholders[8].start == 31 && placeholders[8].end == 42 && placeholders[8].textModifier == InlineCode)
        assert(placeholders[9].start == 43 && placeholders[9].end == 44 && placeholders[9].textModifier == Spacing)
        assert(placeholders[10].start == 44 && placeholders[10].end == 56 && placeholders[10].textModifier == Superscript)
        assert(placeholders[11].start == 57 && placeholders[11].end == 58 && placeholders[11].textModifier == Spacing)
        assert(placeholders[12].start == 58 && placeholders[12].end == 65 && placeholders[12].textModifier == Spoiler)
        assert(placeholders[13].start == 66 && placeholders[13].end == 67 && placeholders[13].textModifier == Spacing)
        assert(placeholders[14].start == 67 && placeholders[14].end == 74 && placeholders[14].textModifier == Heading(6))
        assert(placeholders[15].start == 76 && placeholders[15].end == 91 && placeholders[15].textModifier == Quote)
        assert(placeholders[16].start == 75 && placeholders[16].end == 76 && placeholders[16].textModifier == Spacing)
        assert(placeholders[17].start == 92 && placeholders[17].end == 93 && placeholders[17].textModifier == Spacing)
        assert(placeholders[18].start == 93 && placeholders[18].end == 109 && placeholders[18].textModifier == UnorderedListItem(1))
        assert(placeholders[19].start == 110 && placeholders[19].end == 130 && placeholders[19].textModifier == UnorderedListItem(2))
        assert(placeholders[20].start == 131 && placeholders[20].end == 147 && placeholders[20].textModifier == UnorderedListItem(1))
        assert(placeholders[21].start == 148 && placeholders[21].end == 149 && placeholders[21].textModifier == Spacing)
        assert(placeholders[22].start == 149 && placeholders[22].end == 163 && placeholders[22].textModifier == OrderedListItem(1, 1))
        assert(placeholders[23].start == 164 && placeholders[23].end == 182 && placeholders[23].textModifier == OrderedListItem(2, 1))
        assert(placeholders[24].start == 183 && placeholders[24].end == 197 && placeholders[24].textModifier == OrderedListItem(1, 2))

    }

    @Test
    fun table(){
        val html = "&lt;div class=\"md\"&gt;&lt;p&gt;&amp;#x200B;&lt;/p&gt;\\n\\n&lt;table&gt;&lt;thead&gt;\\n&lt;tr&gt;\\n&lt;th align=\"left\"&gt;Row 1, Col 1&lt;/th&gt;\\n&lt;th align=\"left\"&gt;Row 1, Col 2&lt;/th&gt;\\n&lt;th align=\"left\"&gt;Row 1, Col 3&lt;/th&gt;\\n&lt;/tr&gt;\\n&lt;/thead&gt;&lt;tbody&gt;\\n&lt;tr&gt;\\n&lt;td align=\"left\"&gt;Row 2, Col 1&lt;/td&gt;\\n&lt;td align=\"left\"&gt;Row 2, Col 2&lt;/td&gt;\\n&lt;td align=\"left\"&gt;Row 2, Col 3&lt;/td&gt;\\n&lt;/tr&gt;\\n&lt;/tbody&gt;&lt;/table&gt;\\n&lt;/div&gt;"
        val segments = html.parseToHtmlSegments()
        assert(segments.size == 1 && segments[0] is Table)

        val table = segments[0] as Table

        // Verify Headers
        val headers = table.headers
        assert(headers.size == 3)
        assert(headers[0].simpleText.text == "Row 1, Col 1")
        assert(headers[1].simpleText.text == "Row 1, Col 2")
        assert(headers[2].simpleText.text == "Row 1, Col 3")

        assert(table.cellRows.size == 1)
        val row1 = table.cellRows[0]
        assert(row1.size == 3)
        assert(row1[0].simpleText.text == "Row 2, Col 1")
        assert(row1[1].simpleText.text == "Row 2, Col 2")
        assert(row1[2].simpleText.text == "Row 2, Col 3")
    }

    @Test
    fun codeBlock(){
        val html = "&lt;div class=\"md\"&gt;&lt;pre&gt;&lt;code&gt;println(&amp;quot;Hello, world!&amp;quot;)\\n&lt;/code&gt;&lt;/pre&gt;\\n&lt;/div&gt;"
        val segments = html.parseToHtmlSegments()
        assert(segments.size == 1 && segments[0] is CodeBlock)

        val codeBlock = segments[0] as CodeBlock
        assert(codeBlock.text == "println(\"Hello, world!\")")
    }

}