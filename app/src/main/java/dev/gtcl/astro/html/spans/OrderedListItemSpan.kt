package dev.gtcl.astro.html.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan
import java.util.*

class OrderedListItemSpan(
    private val leadWidth: Int,
    private val gapWidth: Int,
    private val item: OrderedListItem
) : LeadingMarginSpan {

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
            val leadingText = "${item.getStringValue()}."
            val orgStyle = p.style
            p.style = Paint.Style.FILL
            val width = p.measureText(leadingText)
            c.drawText(
                leadingText,
                ((leadWidth * item.depth) + x - width / 2) * dir.toFloat(),
                bottom - p.descent(),
                p
            )
            p.style = orgStyle
        }
    }

    private fun OrderedListItem.getStringValue(): String {
        return when (depth) {
            1 -> value.toString()
            2 -> value.toExcelColumn()
            else -> value.toRomanNumeral()
        }
    }

    private fun Int.toRomanNumeral(uppercase: Boolean = false): String {
        val m = listOf("", "m", "mm", "mmm")
        val c = listOf("", "c", "cc", "ccc", "cd", "d", "dc", "dcc", "dccc", "cm")
        val x = listOf("", "x", "xx", "xxx", "xl", "l", "lx", "lxx", "lxxx", "xc")
        val i = listOf("", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix")

        val result =
            "${m[this / 1000]}${c[(this % 1000) / 100]}${x[(this % 100) / 10]}${i[this % 10]}"

        return if (uppercase) {
            result.toUpperCase(Locale.ENGLISH)
        } else {
            result
        }
    }

    private fun Int.toExcelColumn(uppercase: Boolean = false): String {
        val sb = StringBuilder()
        var num = this
        while (num > 0) {
            val modulo = (num - 1) % 26
            sb.insert(0, 'a' + modulo)
            num = (num - modulo) / 26
        }
        val result = sb.toString()
        return if (uppercase) {
            result.toUpperCase(Locale.ENGLISH)
        } else {
            result
        }
    }
}