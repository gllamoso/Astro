package dev.gtcl.astro

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.URLUtil
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.html.toDp
import dev.gtcl.astro.models.reddit.RuleFor
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.fragments.multireddits.MultiRedditSubredditsAdapter
import dev.gtcl.astro.url.UrlType
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("loadImage")
fun loadImage(imgView: ImageView, imgUrl: String?) {
    when {
        imgUrl == null -> imgView.visibility = View.GONE
        !URLUtil.isValidUrl(imgUrl) && !Patterns.WEB_URL.matcher(imgUrl).matches() -> {
            imgView.apply {
                visibility = View.VISIBLE
                setImageResource(R.drawable.ic_no_photo_24)
                setBackgroundColor(Color.GRAY)
            }
        }
        else -> {
            imgView.visibility = View.VISIBLE
            GlideApp.with(imgView.context)
                .load(imgUrl)
                .error(R.drawable.ic_broken_image_24)
                .addListener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (resource is GifDrawable) {
                            imgView.setImageBitmap(resource.firstFrame)
                            return true
                        }
                        return false
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
//            .skipMemoryCache(true)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .apply(
//                RequestOptions()
//                    .error(R.drawable.ic_broken_image_24)
//            )
                .into(imgView)
        }
    }
}

@BindingAdapter("banner")
fun loadBanner(imgView: ImageView, url: String?) {
    if (!url.isNullOrBlank() && URLUtil.isValidUrl(url) && Patterns.WEB_URL.matcher(url)
            .matches()
    ) {
        GlideApp.with(imgView.context)
            .load(url)
            .skipMemoryCache(true)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .fitCenter()
            .into(imgView)
    }
}

@BindingAdapter("uri")
fun bindUriToImage(imgView: ImageView, uri: Uri?) {
    if (uri == null) {
        imgView.visibility = View.GONE
        return
    }

    imgView.visibility = View.VISIBLE
    GlideApp.with(imgView.context)
        .load(uri)
        .skipMemoryCache(true)
//        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .into(imgView)
}

class SubsamplingScaleImageViewTarget(view: SubsamplingScaleImageView) :
    CustomViewTarget<SubsamplingScaleImageView, Bitmap>(view) {
    override fun onLoadFailed(errorDrawable: Drawable?) {}

    override fun onResourceCleared(placeholder: Drawable?) {}

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        view.setImage(ImageSource.bitmap(resource))
    }
}

@BindingAdapter("listingType")
fun loadMultiIcon(imgView: ImageView, postListing: PostListing) {
    when (postListing) {
        FrontPage -> imgView.setImageResource(R.drawable.ic_front_page_24)
        All -> imgView.setImageResource(R.drawable.ic_all_24)
        Popular -> imgView.setImageResource(R.drawable.ic_trending_up_24)
        Friends -> imgView.setImageResource(R.drawable.ic_people_24)
        is ProfileListing -> imgView.setImageResource(R.drawable.ic_bookmark_24)
        is MultiRedditListing -> imgView.setImageResource(R.drawable.ic_collection_24)
        else -> imgView.setImageResource(R.drawable.ic_saturn_24)
    }
}

@BindingAdapter("subredditIcon")
fun loadSubIcon(imgView: ImageView, imgUrl: String?) {
    if (imgUrl == null || !imgUrl.startsWith("http")) {
        imgView.setImageResource(R.drawable.ic_saturn_colored_24)
    } else {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        GlideApp.with(imgView.context)
            .load(imgUri)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_saturn_colored_24)
                    .circleCrop()
            )
            .into(imgView)
    }
}

@BindingAdapter("accountIcon")
fun loadAccountIcon(imgView: ImageView, imgUrl: String?) {
    if (imgUrl == null || !imgUrl.startsWith("http")) {
        imgView.setImageResource(R.drawable.ic_profile_24)
    } else {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        GlideApp.with(imgView.context)
            .load(imgUri)
            .skipMemoryCache(true)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_profile_24)
                    .circleCrop()
            )
            .into(imgView)
    }
}

@BindingAdapter("multiIcon")
fun loadMultiRedditIcon(imgView: ImageView, imgUrl: String?) {
    if (imgUrl == null || !imgUrl.startsWith("http")) {
        imgView.setImageResource(R.drawable.ic_collection_24)
    } else {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        GlideApp.with(imgView.context)
            .load(imgUri)
            .skipMemoryCache(true)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.ic_collection_24)
                    .circleCrop()
            )
            .into(imgView)
    }
}

@BindingAdapter("subscriptionIcon")
fun loadSubscriptionIcon(imgView: ImageView, subscription: Subscription) {
    val placeHolder = when (subscription.type) {
        SubscriptionType.MULTIREDDIT -> R.drawable.ic_collection_24
        SubscriptionType.USER -> R.drawable.ic_profile_24
        SubscriptionType.SUBREDDIT -> R.drawable.ic_saturn_24
    }

    if (subscription.icon == null || !subscription.icon.startsWith("https", true)) {
        imgView.setImageResource(placeHolder)
        return
    }

    GlideApp.with(imgView.context)
        .load(subscription.icon)
//        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .apply(
            RequestOptions()
                .placeholder(placeHolder)
                .circleCrop()
        )
        .into(imgView)
}


@BindingAdapter("colorTint")
fun setTint(imgView: ImageView, color: Int) {
    imgView.setColorFilter(color)
}

@BindingAdapter("favorite")
fun loadFavoriteIcon(imgView: ImageView, isFavorite: Boolean) {
    imgView.setImageResource(if (isFavorite) R.drawable.ic_favorite_filled_24 else R.drawable.ic_favorite_unfilled_24)
}

@BindingAdapter("listingType")
fun loadListingText(txtView: TextView, postListing: PostListing?) {
    val text = getListingTitle(txtView.context, postListing)
    txtView.text = text
}

@BindingAdapter("listingType")
fun setVisibility(viewGroup: ViewGroup, postListing: PostListing?) {
    if (postListing == null) return
    viewGroup.visibility = if (postListing is SubredditListing) View.VISIBLE else View.GONE
}

@BindingAdapter("indent")
fun setIndentation(linearLayout: LinearLayout, indent: Int) {
    linearLayout.removeAllViews()
    val viewSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        .90F,
        linearLayout.context.resources.displayMetrics
    ).toInt()
    val indentationSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        12F,
        linearLayout.context.resources.displayMetrics
    ).toInt()
    val params =
        LinearLayout.LayoutParams(viewSize, WindowManager.LayoutParams.MATCH_PARENT).apply {
            marginStart = indentationSize
        }
    for (i in 1..indent) {
        val view = View(linearLayout.context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.commentDivider))
            layoutParams = params
        }
        linearLayout.addView(view)
    }
}

@BindingAdapter("moreComment")
fun setMoreCommentText(textView: TextView, item: More) {
    if (item.isContinueThreadLink)
        textView.text = textView.resources.getString(R.string.continue_to_thread)
    else
        textView.text = String.format(
            textView.resources.getText(R.string.more_replies).toString(),
            item.childrenLeft
        )
}

@BindingAdapter("timestamp")
fun setTimestamp(textView: TextView, time: Long?) {
    if (time != null) {
        val timestamp = timeSince(time)
        textView.text = timestamp
    } else {
        textView.text = ""
    }
}

@BindingAdapter("secondsToDate")
fun secondsToDate(textView: TextView, time: Long) {
    val simpleDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    textView.text = simpleDateFormat.format(1000L * time)
}

@BindingAdapter("read")
fun setTextColor(textView: TextView, isRead: Boolean?) {
    if (isRead == true) {
        textView.setTextColor(ContextCompat.getColor(textView.context, android.R.color.darker_gray))
    } else {
        textView.setTextColor(textView.context.resolveColorAttr(android.R.attr.textColorPrimary))
    }
}

@BindingAdapter("invert")
fun invert(view: View, inverted: Boolean?) {
    if (inverted == null) {
        view.visibility = View.GONE
    } else {
        view.visibility = View.VISIBLE
        view.rotation = if (inverted) {
            180F
        } else {
            0F
        }
    }
}

@BindingAdapter("subredditsInMultireddit")
fun bindRecyclerViewForMultiReddit(recyclerView: RecyclerView, data: MutableList<Subreddit>) {
    val adapter = recyclerView.adapter as MultiRedditSubredditsAdapter
    adapter.submitList(data)
}

@BindingAdapter("likes")
fun applyLikeTint(imageView: ImageView, likes: Boolean?) {
    when (likes) {
        true -> imageView.setColorFilter(
            ContextCompat.getColor(
                imageView.context,
                android.R.color.holo_orange_dark
            )
        )
        false -> {
            imageView.setColorFilter(
                ContextCompat.getColor(
                    imageView.context,
                    android.R.color.holo_blue_dark
                )
            )
        }
        null -> imageView.clearColorFilter()
    }
}

@BindingAdapter("upvoteTint")
fun applyUpvoteTint(imageView: ImageView, likes: Boolean?) {
    when (likes) {
        true -> imageView.setColorFilter(
            ContextCompat.getColor(
                imageView.context,
                android.R.color.holo_orange_dark
            )
        )
        else -> imageView.clearColorFilter()
    }
}

@BindingAdapter("downvoteTint")
fun applyDownvoteTint(imageView: ImageView, likes: Boolean?) {
    when (likes) {
        false -> {
            imageView.setColorFilter(
                ContextCompat.getColor(
                    imageView.context,
                    android.R.color.holo_blue_dark
                )
            )
        }
        else -> imageView.clearColorFilter()
    }
}

@BindingAdapter("bookmarkTint")
fun applyBookmarkTint(imageView: ImageView, bookmarked: Boolean) {
    when (bookmarked) {
        true -> imageView.setColorFilter(
            ContextCompat.getColor(
                imageView.context,
                android.R.color.holo_orange_light
            )
        )
        else -> imageView.clearColorFilter()
    }
}

@BindingAdapter("itemWithSmallFlair")
fun setSmallFlairWithItem(cardView: MaterialCardView, item: Item?) {
    val darkTextColor = item is Post && item.flairTextColor == "dark"
    val flairs = when (item) {
        is Post -> item.flairRichtext
        is Comment -> item.flairRichtext
        else -> null
    }
    if (flairs.isNullOrEmpty()) {
        val text = when (item) {
            is Post -> item.flairText?.removeHtmlEntities()
            is Comment -> item.authorFlairText?.removeHtmlEntities()
            else -> null
        }
        setFlairText(cardView, text, darkTextColor, true)
    } else {
        setFlairRichText(cardView, flairs, darkTextColor, true)
    }

}

@BindingAdapter("itemWithFlair")
fun setFlairWithItem(cardView: MaterialCardView, item: Item?) {
    val darkTextColor = item is Post && item.flairTextColor == "dark"
    val flairs = when (item) {
        is Post -> item.flairRichtext
        is Comment -> item.flairRichtext
        else -> null
    }

    if (flairs.isNullOrEmpty()) {
        val text = when (item) {
            is Post -> item.flairText?.removeHtmlEntities()
            is Comment -> item.authorFlairText?.removeHtmlEntities()
            else -> null
        }
        setFlairText(cardView, text, darkTextColor, false)
    } else {
        setFlairRichText(cardView, flairs, darkTextColor, false)
    }
}

@BindingAdapter("flair")
fun setFlairLayout(cardView: MaterialCardView, flair: Flair?) {
    cardView.removeAllViews()
    if (flair != null) {
        if (flair.richtext.isNullOrEmpty()) {
            setFlairText(
                cardView,
                flair.text.removeHtmlEntities(),
                darkTextColor = false,
                small = false
            )
        } else {
            setFlairRichText(cardView, flair.richtext, darkTextColor = false, small = false)
        }
    }
}

private fun setFlairText(
    cardView: MaterialCardView,
    text: String?,
    darkTextColor: Boolean,
    small: Boolean
) {
    val context = cardView.context
    cardView.removeAllViews()
    val textColor = if (darkTextColor) {
        Color.BLACK
    } else {
        Color.WHITE
    }
    val padding = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        4f,
        context.resources.displayMetrics
    ).toInt()

    if (!text.isNullOrBlank() && text.trim() != "\u200B") {
        val textView = TextView(context).apply {
            this.text = text.trim()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (small) 12f else 14f)
            setTextColor(textColor)
            isSingleLine = true
            setPadding(padding, padding / 2, padding, padding / 2)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                this.gravity = Gravity.CENTER
            }
        }
        cardView.apply {
            visibility = View.VISIBLE
            addView(textView)
        }
    } else {
        cardView.visibility = View.GONE
    }
}

private fun setFlairRichText(
    cardView: MaterialCardView,
    flairs: List<FlairRichtext>?,
    darkTextColor: Boolean,
    small: Boolean
) {
    cardView.removeAllViews()
    val textColor = if (darkTextColor) {
        Color.BLACK
    } else {
        Color.WHITE
    }
    if (!flairs.isNullOrEmpty()) {
        val context = cardView.context
        val imgViewSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            if (small) 14f else 18f,
            context.resources.displayMetrics
        ).toInt()
        val horizontalMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            context.resources.displayMetrics
        ).toInt()
        val verticalMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            context.resources.displayMetrics
        ).toInt()
        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                this.gravity = Gravity.CENTER
            }
        }
        var start = true
        var layoutHasView = false
        for (flair in flairs) {
            if (!flair.urlFormatted.isNullOrBlank() || flair.textFormatted.toString()
                    .isNotBlank()
            ) {
                val view =
                    if (!flair.urlFormatted.isNullOrBlank()) {
                        ImageView(context).apply {
                            layoutParams =
                                LinearLayout.LayoutParams(imgViewSize, imgViewSize).apply {
                                    if (!start) {
                                        marginStart = horizontalMargin
                                    }
                                }
                            loadImage(this, flair.urlFormatted)
                        }
                    } else {
                        TextView(context).apply {
                            text = flair.textFormatted.toString().trim()
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (small) 12f else 14f)
                            setTextColor(textColor)
                            isSingleLine = true
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                if (!start) {
                                    marginStart = horizontalMargin
                                }
                            }
                        }
                    }

                linearLayout.addView(view)
                layoutHasView = true
                start = false
            }
        }
        cardView.apply {
            visibility = if (layoutHasView) {
                addView(linearLayout)
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    } else {
        cardView.visibility = View.GONE
    }
}

@BindingAdapter("flairBackground")
fun setFlairBackground(cardView: MaterialCardView, color: String?) {
    val backgroundColor = when (color) {
        "", null -> ContextCompat.getColor(cardView.context, android.R.color.darker_gray)
        else -> Color.parseColor(color)
    }
    cardView.setCardBackgroundColor(backgroundColor)
}

@BindingAdapter("ruleType")
fun setRuleTypeText(textView: TextView, ruleFor: RuleFor) {
    val context = textView.context ?: return
    textView.text = when (ruleFor) {
        RuleFor.POST -> context.getText(R.string.posts)
        RuleFor.COMMENT -> context.getText(R.string.comments)
        RuleFor.ALL -> context.getText(R.string.posts_and_comments)
    }
}

@BindingAdapter("flair")
fun setFlairOnChip(chip: Chip, flair: Flair?) {
    if (flair == null) {
        chip.isChecked = false
        chip.text = chip.context.getText(R.string.no_flair)
    } else {
        chip.isChecked = true
        chip.text = flair.text
    }
}

@BindingAdapter("postSort")
fun setSortText(textView: TextView, postSort: PostSort) {
    textView.text = textView.context.getText(
        when (postSort) {
            PostSort.BEST -> R.string.order_best
            PostSort.HOT -> R.string.order_hot
            PostSort.NEW -> R.string.order_new
            PostSort.TOP -> R.string.order_top
            PostSort.CONTROVERSIAL -> R.string.order_controversial
            PostSort.RISING -> R.string.order_rising
            PostSort.RELEVANCE -> R.string.order_most_relevant
            PostSort.COMMENTS -> R.string.order_comment_count
        }
    )
}

@BindingAdapter("time")
fun setTimeText(textView: TextView, time: Time?) {
    time?.let {
        textView.text = textView.context.getText(
            when (it) {
                Time.HOUR -> R.string.past_hour
                Time.DAY -> R.string.past_24_hours
                Time.WEEK -> R.string.past_week
                Time.MONTH -> R.string.past_month
                Time.YEAR -> R.string.past_year
                Time.ALL -> R.string.all_time
            }
        )
    }
}

@BindingAdapter("commentSort")
fun setCommentSortText(textView: TextView, commentSort: CommentSort) {
    textView.text = textView.context.getText(
        when (commentSort) {
            CommentSort.BEST -> R.string.order_best
            CommentSort.TOP -> R.string.order_top
            CommentSort.NEW -> R.string.order_new
            CommentSort.CONTROVERSIAL -> R.string.order_controversial
            CommentSort.OLD -> R.string.order_old
            CommentSort.RANDOM -> R.string.order_random
            CommentSort.QA -> R.string.order_qanda
        }
    )
}

@BindingAdapter("isUser")
fun setAuthorTextColor(textView: TextView, isUser: Boolean) {
    val context = textView.context ?: return
    val typedValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
    val arr =
        context.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.textColorPrimary))
    textView.setTextColor(
        if (isUser) {
            -5220187
        } else {
            arr.getColor(0, -1)
        }
    )
    arr.recycle()
}

@BindingAdapter("isOp")
fun setAuthorCardBackground(materialCardView: MaterialCardView, isOp: Boolean) {
    val padding = if (isOp) {
        4.toDp(materialCardView.context)
    } else {
        0
    }

    val backgroundColor = if (isOp) {
        -12336129
    } else {
        Color.TRANSPARENT
    }

    materialCardView.setCardBackgroundColor(backgroundColor)
    if (materialCardView.childCount > 0) {
        val child = materialCardView.getChildAt(0)
        child.setPadding(padding, 0, padding, 0)
    }
}

@BindingAdapter("urlType")
fun setUrlTypeIcon(imgView: ImageView, urlType: UrlType?) {
    if (urlType == null) {
        imgView.setImageDrawable(null)
        return
    }

    val imageResource =
        when (urlType) {
            UrlType.IMAGE -> R.drawable.ic_photo_24
            UrlType.GIF -> R.drawable.ic_photo_24
            UrlType.GIFV -> R.drawable.ic_videocam_24
            UrlType.GFYCAT -> R.drawable.ic_videocam_24
            UrlType.REDGIFS -> R.drawable.ic_videocam_24
            UrlType.HLS -> R.drawable.ic_videocam_24
            UrlType.REDDIT_VIDEO -> R.drawable.ic_videocam_24
            UrlType.STANDARD_VIDEO -> R.drawable.ic_videocam_24
            UrlType.IMGUR_ALBUM -> R.drawable.ic_photo_library_24
            UrlType.IMGUR_IMAGE -> R.drawable.ic_photo_24
            UrlType.REDDIT_GALLERY -> R.drawable.ic_photo_library_24
            else -> R.drawable.ic_link_24
        }

    imgView.setImageResource(imageResource)
}

@BindingAdapter("removalReason")
fun setRemovalReason(textView: TextView, reason: String?) {
    when (reason) {
        "moderator" -> textView.setText(R.string.post_removed_by_moderator)
        "admin" -> textView.setText(R.string.post_removed_by_admin)
        null -> textView.text = ""
        else -> textView.setText(R.string.post_removed)
    }
}

@BindingAdapter("awardsNum")
fun setNumberOfAwardsText(textView: TextView, num: Int?) {
    when (num) {
        0, null -> {
            textView.visibility = View.GONE
        }
        else -> {
            textView.apply {
                visibility = View.VISIBLE
                text = String.format(textView.context.getString(R.string.num_awards), num)
            }
        }
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("post")
fun setPostInfo(textView: TextView, post: Post?) {
    if (post == null) {
        textView.visibility = View.GONE
    } else {
        textView.apply {
            visibility = View.VISIBLE
            text = "${post.subredditPrefixed} • ${post.author} • ${post.domain}"
        }
    }
}

@BindingAdapter("cardStrokeWidth")
fun setMaterialCardStrokeWidth(materialCardView: MaterialCardView, strokeWidth: Int?) {
    val context = materialCardView.context
    val width = (strokeWidth ?: 0).toDp(context)
    materialCardView.strokeWidth = width
}