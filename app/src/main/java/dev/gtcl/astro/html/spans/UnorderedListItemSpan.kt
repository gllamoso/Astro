package dev.gtcl.astro.html.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan

class UnorderedListItemSpan(
    private val leadWidth: Int,
    private val gapWidth: Int,
    private val item: UnorderedListItem
) : LeadingMarginSpan {
    // •, ■, ○, ⦿, ⦾, ◦, •, ◘, ◙

    override fun getLeadingMargin(first: Boolean): Int {
        return (leadWidth * item.depth) + gapWidth
    }

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence?,
        start: Int,
        end: Int,
        first: Boolean,
        l: Layout?
    ) {
        if (first) {
            val leadingText = when (item.depth % 2) {
                1 -> "•"
                else -> "◦"
            }
            val orgStyle = p.style
            p.style = Paint.Style.FILL
            val width = p.measureText(leadingText)
            c.drawText(
                leadingText,
                ((leadWidth * item.depth) + x - width / 2) * dir,
                baseline.toFloat(),
                p
            )
            p.style = orgStyle
        }
    }
}