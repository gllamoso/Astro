package dev.gtcl.astro.markdown

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.view.View
import android.widget.TextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolverDef
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.utils.NoCopySpannableFactory

class CustomMarkwonPlugin(private val linkHandler: (String) -> Unit) : AbstractMarkwonPlugin() {

    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
        builder.linkResolver(object : LinkResolverDef() {
            override fun resolve(view: View, link: String) {
                linkHandler(link)
            }
        })
    }

    override fun beforeSetText(textView: TextView, markdown: Spanned) {
        val spannableStringBuilder = markdown as SpannableStringBuilder
        MarkdownToSpannable.setSpannableStringBuilder(
            textView.context,
            spannableStringBuilder,
            textView.currentTextColor
        )
        textView.setSpannableFactory(NoCopySpannableFactory.getInstance())
        super.beforeSetText(textView, markdown)
    }

    override fun afterSetText(textView: TextView) {
        super.afterSetText(textView)
        textView.apply {
            isClickable = false
            isLongClickable = false
        }
    }
}