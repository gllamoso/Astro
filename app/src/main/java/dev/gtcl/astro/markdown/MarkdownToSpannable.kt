package dev.gtcl.astro.markdown

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.SuperscriptSpan
import android.view.View


class MarkdownToSpannable {
    companion object {

        private val SPOILER_REGEX = ">!.+!<".toRegex()
        private val NONSENSE_TEXT_REGEX = "^&(#x200B|nbsp);$".toRegex(RegexOption.MULTILINE)
        private val QUOTE_TEXT_REGEX = "^>.*$".toRegex(RegexOption.MULTILINE)
        private val SUPERSCRIPT_GROUP_REGEX = "\\^\\(.+\\)".toRegex()
        private val SUPERSCRIPT_WORD_REGEX = "\\^[^\\s()]+".toRegex()

        fun setSpannableStringBuilder(
            context: Context,
            spannableStringBuilder: SpannableStringBuilder,
            defaultTextColor: Int
        ) {
            removeNonsenseText(spannableStringBuilder)
            setSpoilersInMarkdown(
                context,
                spannableStringBuilder,
                defaultTextColor
            )
            setSuperscriptInMarkdown(spannableStringBuilder)
            setQuoteMarkdown(spannableStringBuilder)
        }

        private fun setSpoilersInMarkdown(
            context: Context,
            spannableStringBuilder: SpannableStringBuilder,
            defaultColor: Int
        ) {
            var match = SPOILER_REGEX.find(spannableStringBuilder)
            while (match != null) {
                val start = match.range.first
                val end = match.range.last
                spannableStringBuilder.delete(end - 1, end + 1)
                spannableStringBuilder.delete(start, start + 2)
                val backgroundColorSpan =
                    BackgroundColorSpan(context.getColor(android.R.color.darker_gray))
                val foregroundColorSpan =
                    ForegroundColorSpan(context.getColor(android.R.color.darker_gray))
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        spannableStringBuilder.removeSpan(backgroundColorSpan)
                        spannableStringBuilder.removeSpan(foregroundColorSpan)
                        spannableStringBuilder.removeSpan(this)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = defaultColor
                        ds.isUnderlineText = false
                    }
                }
                spannableStringBuilder.setSpan(
                    clickableSpan,
                    start,
                    end - 3,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableStringBuilder.setSpan(
                    backgroundColorSpan,
                    start,
                    end - 3,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableStringBuilder.setSpan(
                    foregroundColorSpan,
                    start,
                    end - 3,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                match = SPOILER_REGEX.find(spannableStringBuilder)
            }
        }

        private fun setQuoteMarkdown(spannableStringBuilder: SpannableStringBuilder) {
            var match = QUOTE_TEXT_REGEX.find(spannableStringBuilder)
            while (match != null) {
                val start = match.range.first
                val end = match.range.last
                val quotePrefix = (">\\s*".toRegex().find(match.value) ?: return).value
                if (spannableStringBuilder.length > start + quotePrefix.length) {
                    spannableStringBuilder.delete(start, start + quotePrefix.length)
                } else {
                    break
                }
                val addedWhiteSpace: Int
                addedWhiteSpace = if (match.value == quotePrefix) {
                    spannableStringBuilder.insert(start, " ")
                    2
                } else {
                    0
                }

                if (end - quotePrefix.length + addedWhiteSpace - start != 0) {
                    spannableStringBuilder.setSpan(
                        CustomQuoteSpan(Color.GREEN, 10, 40),
                        start,
                        end - quotePrefix.length + addedWhiteSpace,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                match = QUOTE_TEXT_REGEX.find(spannableStringBuilder)
            }
        }

        private fun setSuperscriptInMarkdown(spannableStringBuilder: SpannableStringBuilder) {
            var match = SUPERSCRIPT_GROUP_REGEX.find(spannableStringBuilder)
            while (match != null) {
                val start = match.range.first
                val end = match.range.last

                spannableStringBuilder.delete(end, end + 1)
                spannableStringBuilder.delete(start, start + 2)
                if (end - 2 - start != 0) {
                    spannableStringBuilder.setSpan(
                        SuperscriptSpan(),
                        start,
                        end - 2,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                match = SUPERSCRIPT_GROUP_REGEX.find(spannableStringBuilder)
            }

            match = SUPERSCRIPT_WORD_REGEX.find(spannableStringBuilder)
            while(match != null){
                val start = match.range.first
                val end = match.range.last

                spannableStringBuilder.delete(start, start + 1)
                if (end - start != 0) {
                    spannableStringBuilder.setSpan(
                        SuperscriptSpan(),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                match = SUPERSCRIPT_WORD_REGEX.find(spannableStringBuilder)
            }
        }

        private fun removeNonsenseText(spannableStringBuilder: SpannableStringBuilder) {
            var match = NONSENSE_TEXT_REGEX.find(spannableStringBuilder)
            while (match != null) {
                val start = match.range.first
                val end = match.range.last
                spannableStringBuilder.delete(start, end + 1)
                match = NONSENSE_TEXT_REGEX.find(spannableStringBuilder)
            }
        }
    }
}