package dev.gtcl.astro.html

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.*
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.core.view.setPadding
import dev.gtcl.astro.R
import dev.gtcl.astro.actions.LinkHandler
import dev.gtcl.astro.html.spans.*
import me.saket.bettermovementmethod.BetterLinkMovementMethod

fun SimpleText.createView(context: Context, linkHandler: LinkHandler): TextView {
    val margin = 8.toDp(context)
    val textView = TextView(context)
    val spannableString = createSpannableString(
        context,
        text,
        spanPlaceholders,
        textView.currentTextColor,
        linkHandler
    )
    return textView.apply {
//        setPadding(margin)
        setLineSpacing(margin / 4F, 1F)
        text = spannableString
        movementMethod = BetterLinkMovementMethod.getInstance()
    }
}

fun SimpleText.createCellView(context: Context, linkHandler: LinkHandler): TextView {
    val margin = 8.toDp(context)
    val textView = TextView(context)
    val spannableString = createSpannableString(
        context,
        text,
        spanPlaceholders,
        textView.currentTextColor,
        linkHandler
    )
    return textView.apply {
        setPadding(margin)
        text = spannableString
        setLineSpacing(margin / 4F, 1F)
        setBackgroundResource(R.drawable.cell_shape)
        movementMethod = BetterLinkMovementMethod.getInstance()
        isClickable = false
        isFocusable = false
        isLongClickable = false
    }
}

fun CodeBlock.createView(context: Context, linkHandler: LinkHandler): TextView {
    val margin = 8.toDp(context)
    val textView = TextView(context)
    val spannableString = createSpannableString(
        context,
        text,
        spanPlaceholders,
        textView.currentTextColor,
        linkHandler
    )
    return textView.apply {
        setPadding(margin)
        setLineSpacing(margin / 4F, 1F)
        setBackgroundResource(android.R.color.darker_gray)
        typeface = Typeface.MONOSPACE
        text = spannableString
        movementMethod = BetterLinkMovementMethod.getInstance()
    }
}

fun Table.createView(context: Context, linkHandler: LinkHandler): TableLayout {
    val tableLayout = TableLayout(context).apply {
        setPadding(8.toDp(context))
        isClickable = false
        isLongClickable = false
        isFocusable = false
    }

    val headerRow = TableRow(context).apply {
        isClickable = false
        isLongClickable = false
        isFocusable = false
    }
    for ((text, alignment) in headers) {
        val textView = text.createCellView(
            context, linkHandler
        ).apply {
            textAlignment = when (alignment) {
                Cell.Alignment.CENTER -> View.TEXT_ALIGNMENT_CENTER
                Cell.Alignment.LEFT -> View.TEXT_ALIGNMENT_VIEW_START
                Cell.Alignment.RIGHT -> View.TEXT_ALIGNMENT_VIEW_END
            }
//            setTypeface(this.typeface, Typeface.BOLD)
        }
        headerRow.addView(textView)
    }
    tableLayout.addView(headerRow)

    for (row in cellRows) {
        if (row.isNotEmpty()) {
            val tableRow = TableRow(context).apply {
                isClickable = false
                isLongClickable = false
                isFocusable = false
            }
            for ((text, alignment) in row) {
                val textView = text.createCellView(context, linkHandler).apply {
                    textAlignment = when (alignment) {
                        Cell.Alignment.CENTER -> View.TEXT_ALIGNMENT_CENTER
                        Cell.Alignment.LEFT -> View.TEXT_ALIGNMENT_VIEW_START
                        Cell.Alignment.RIGHT -> View.TEXT_ALIGNMENT_VIEW_END
                    }
                }
                tableRow.addView(textView)
            }
            tableLayout.addView(tableRow)
        }
    }


    return tableLayout
}

fun createSpannableString(
    context: Context,
    str: String,
    spanPlaceholders: List<SpanPlaceholder>,
    defaultTextColor: Int,
    linkHandler: LinkHandler
): SpannableString {
    val spannableString = SpannableString(str)

    for ((start, end, item) in spanPlaceholders) {
        when (item) {
            Bold -> spannableString.setSpan(StyleSpan(Typeface.BOLD), start, end)
            Italicize -> spannableString.setSpan(StyleSpan(Typeface.ITALIC), start, end)
            Strikethrough -> spannableString.setSpan(StrikethroughSpan(), start, end)
            Spoiler -> {
                val backgroundColorSpan =
                    BackgroundColorSpan(context.getColor(android.R.color.darker_gray))
                val foregroundColorSpan =
                    ForegroundColorSpan(context.getColor(android.R.color.darker_gray))
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        spannableString.removeSpan(backgroundColorSpan)
                        spannableString.removeSpan(foregroundColorSpan)
                        spannableString.removeSpan(this)
                        (widget as TextView).text = spannableString
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = defaultTextColor
                        ds.isUnderlineText = false
                    }
                }
                spannableString.apply {
                    setSpan(clickableSpan, start, end)
                    setSpan(backgroundColorSpan, start, end)
                    setSpan(foregroundColorSpan, start, end)
                }
            }
            InlineCode -> {
                spannableString.apply {
                    setSpan(
                        BackgroundColorSpan(context.getColor(android.R.color.darker_gray)),
                        start,
                        end
                    )
                    setSpan(CustomTypefaceSpan(Typeface.MONOSPACE), start, end)
                }
            }
            is Hyperlink -> {
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(p0: View) {
                        linkHandler.handleLink(item.link)
                    }
                }
                spannableString.setSpan(clickableSpan, start, end)
            }
            Superscript -> {
                spannableString.setSpan(SuperscriptSpan(), start, end)
                spannableString.setSpan(RelativeSizeSpan(0.75f), start, end)
            }
            Quote -> {
                val margin = 8.toDp(context)
                spannableString.setSpan(
                    CustomQuoteSpan(Color.LTGRAY, margin / 4, margin),
                    start,
                    end
                )
            }
            is Heading -> {
                val proportion = 1f + (item.size.toFloat() / 6)
                spannableString.setSpan(RelativeSizeSpan(proportion), start, end)
            }
            is UnorderedListItem -> {
                val leadWidth = 16.toDp(context)
                spannableString.setSpan(
                    UnorderedListItemSpan(leadWidth, leadWidth, item),
                    start,
                    end
                )
            }
            is OrderedListItem -> {
                val leadWidth = 16.toDp(context)
                spannableString.setSpan(OrderedListItemSpan(leadWidth, leadWidth, item), start, end)
            }
        }
    }

    return spannableString
}

fun SpannableString.setSpan(what: Any, start: Int, end: Int) {
    setSpan(what, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}

fun HorizontalLine.createView(context: Context): View {
    return View(context).apply {
        setBackgroundResource(android.R.color.darker_gray)
    }
}

@SuppressLint("ClickableViewAccessibility")
fun LinearLayout.createHtmlViews(htmlSegments: List<ParsedHtmlSegment>, linkHandler: LinkHandler) {
    if (htmlSegments.isEmpty()) {
        this.visibility = View.GONE
        return
    }

    this.removeAllViews()
    val context = this.context
    val margin = 8.toDp(context)
    for (i in htmlSegments.indices) {
        val segment = htmlSegments[i]
        val view = when (segment) {
            is SimpleText -> segment.createView(context, linkHandler)
            is CodeBlock -> segment.createView(context, linkHandler)
            is Table -> {
                val tableLayout = segment.createView(context, linkHandler)
                val horizontalScrollView = HorizontalScrollView(context)
                horizontalScrollView.apply {
                    overScrollMode = View.OVER_SCROLL_NEVER
                    addView(tableLayout)
                    var isScrolling: Boolean? = null
                    val detector = GestureDetector(context, object : GestureDetector.OnGestureListener {

                        override fun onDown(p0: MotionEvent?): Boolean = false

                        override fun onShowPress(p0: MotionEvent?) {}

                        override fun onSingleTapUp(p0: MotionEvent?): Boolean {
                            (this@createHtmlViews.parent as View).performClick()
                            return true
                        }

                        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                            if(isScrolling == null){
                                if(distanceX < 0){
                                    val canScrollToLeft = horizontalScrollView.canScrollHorizontally(-1)
                                    isScrolling = canScrollToLeft
                                } else if(distanceX > 0){
                                    val canScrollToRight = horizontalScrollView.canScrollHorizontally(1)
                                    isScrolling = canScrollToRight
                                }
                            }

                            return isScrolling == false
                        }

                        override fun onLongPress(p0: MotionEvent?) {}

                        override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float) = false
                    })
                    setOnTouchListener { _, event ->
                        if(event.actionMasked == MotionEvent.ACTION_CANCEL || event.actionMasked == MotionEvent.ACTION_UP){
                            isScrolling = null
                        }
                        detector.onTouchEvent(event)
                        isScrolling == false
                    }
                }
            }
            is HorizontalLine -> segment.createView(context)
        }

        val layoutParams = if (segment is HorizontalLine) {
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                margin / 16
            )
        } else {
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        if (i != 0) {
            layoutParams.topMargin = margin
        }

        this.addView(view.apply {
            this.layoutParams = layoutParams
            isClickable = false
            isFocusable = false
            isLongClickable = false
        })
    }
}

fun Int.toDp(context: Context): Int {
    val scale = context.resources.displayMetrics.density
    return (this * scale + 0.5F).toInt()
}