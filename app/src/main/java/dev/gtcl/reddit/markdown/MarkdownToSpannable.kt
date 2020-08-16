package dev.gtcl.reddit.markdown

import android.R
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.view.View

val SPOILER_REGEX = ">!.+!<".toRegex()
val STRIKETHROUGH_REGEX = "~~.+~~".toRegex()
class MarkdownToSpannable{
    companion object{
        fun setSpannableStringBuilder(context: Context, spannableStringBuilder: SpannableStringBuilder, defaultTextColor: Int){
            setSpoilersInMarkdown(
                context,
                spannableStringBuilder,
                defaultTextColor
            )
            setStrikethroughMarkdown(
                spannableStringBuilder
            )
        }

        private fun setSpoilersInMarkdown(context: Context, spannableStringBuilder: SpannableStringBuilder, defaultColor: Int){
            var match = SPOILER_REGEX.find(spannableStringBuilder)
            while(match != null){
                val start = match.range.first
                val end = match.range.last
                spannableStringBuilder.delete(end - 1, end + 1)
                spannableStringBuilder.delete(start, start + 2)
                val backgroundColorSpan =
                    BackgroundColorSpan(context.getColor(R.color.darker_gray))
                val foregroundColorSpan =
                    ForegroundColorSpan(context.getColor(R.color.darker_gray))
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

        private fun setStrikethroughMarkdown(spannableStringBuilder: SpannableStringBuilder){
            var match = STRIKETHROUGH_REGEX.find(spannableStringBuilder)
            while(match != null){
                val start = match.range.first
                val end = match.range.last
                spannableStringBuilder.delete(end - 1, end + 1)
                spannableStringBuilder.delete(start, start + 2)
                spannableStringBuilder.setSpan(StrikethroughSpan(), start, end - 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                match = STRIKETHROUGH_REGEX.find(spannableStringBuilder)
            }
        }
    }
}