package dev.gtcl.astro

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Patterns
import android.util.TypedValue
import android.view.*
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.material.chip.Chip
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.databinding.IconFlairBinding
import dev.gtcl.astro.databinding.IconFlairSmallBinding
import dev.gtcl.astro.models.reddit.RuleFor
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.fragments.multireddits.MultiRedditSubredditsAdapter
import java.text.SimpleDateFormat
import java.util.*


@BindingAdapter("loadImageAndHideIfNull")
fun loadImageAndHideIfNull(imgView: ImageView, imgUrl: String?) {
    if (imgUrl != null && URLUtil.isValidUrl(imgUrl) && Patterns.WEB_URL.matcher(imgUrl)
            .matches()
    ) {
        imgView.visibility = View.VISIBLE
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        GlideApp.with(imgView.context)
            .load(imgUri)
            .skipMemoryCache(true)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .apply(
//                RequestOptions()
//                    .error(R.drawable.ic_broken_image_24)
//            )
            .into(imgView)
    } else {
        imgView.visibility = View.GONE
    }
}

@BindingAdapter("loadImage")
fun loadImage(imgView: ImageView, imgUrl: String?) {
    if (imgUrl.isNullOrBlank()) return
    if (imgUrl.startsWith("http")) {
        imgView.visibility = View.VISIBLE
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        GlideApp.with(imgView.context)
            .load(imgUri)
//            .skipMemoryCache(true)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .apply(
//                RequestOptions()
//                    .error(R.drawable.ic_broken_image_24)
//            )
            .into(imgView)
    }
}

@BindingAdapter("banner")
fun loadBanner(imgView: ImageView, url: String?) {
    if (!url.isNullOrBlank()) {
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
            setBackgroundColor(Color.GRAY)
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

@BindingAdapter("viewSize")
fun setViewSize(view: View, percentOfDeviceHeight: Int) {
    val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        view.context.display ?: return
    } else {
        val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay
    }
    val size = Point()
    display.getRealSize(size)
    val height = size.y * percentOfDeviceHeight / 100

    val layoutParams = view.layoutParams
    layoutParams.height = height
    view.layoutParams = layoutParams
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
        null -> imageView.clearColorFilter()
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
        null -> imageView.clearColorFilter()
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

@BindingAdapter("flairListSmall")
fun addSmallFlairList(viewGroup: LinearLayout, list: List<AuthorFlairRichtext>?) {
    viewGroup.removeAllViews()
    if (!list.isNullOrEmpty()) {
        viewGroup.visibility = View.VISIBLE
        val context = viewGroup.context
        val imgViewSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            18f,
            context.resources.displayMetrics
        ).toInt()
        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            context.resources.displayMetrics
        ).toInt()
        val layoutInflater = LayoutInflater.from(context)
        for (flair in list) {
            if (!flair.urlFormatted.isNullOrBlank() || flair.text.toString().isNotBlank()) {
                val view =
                    if (!flair.urlFormatted.isNullOrBlank()) {
                        ImageView(context).apply {
                            layoutParams =
                                LinearLayout.LayoutParams(imgViewSize, imgViewSize).apply {
                                    marginEnd = margin
                                }
                            loadImage(this, flair.urlFormatted)
                        }
                    } else {
                        IconFlairSmallBinding.inflate(layoutInflater).apply {
                            charSequence = flair.text.toString()
                            executePendingBindings()
                        }.root
                    }

                viewGroup.addView(view)
            }
        }
    } else {
        viewGroup.visibility = View.GONE
    }
}

@BindingAdapter("flairList")
fun addFlairList(viewGroup: LinearLayout, list: List<AuthorFlairRichtext>?) {
    viewGroup.removeAllViews()
    if (!list.isNullOrEmpty()) {
        viewGroup.visibility = View.VISIBLE
        val context = viewGroup.context
        val imgViewSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            24f,
            context.resources.displayMetrics
        ).toInt()
        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            context.resources.displayMetrics
        ).toInt()
        val layoutInflater = LayoutInflater.from(context)
        for (flair in list) {
            if (!flair.urlFormatted.isNullOrBlank() || flair.text.toString().isNotBlank()) {
                val view =
                    if (!flair.urlFormatted.isNullOrBlank()) {
                        ImageView(context).apply {
                            layoutParams =
                                LinearLayout.LayoutParams(imgViewSize, imgViewSize).apply {
                                    marginEnd = margin
                                }
                            loadImage(this, flair.urlFormatted)
                        }
                    } else {
                        IconFlairBinding.inflate(layoutInflater).apply {
                            charSequence = flair.text.toString()
                            executePendingBindings()
                        }.root
                    }

                viewGroup.addView(view)
            }
        }
    } else {
        viewGroup.visibility = View.GONE
    }
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
fun setUserTextColor(textView: TextView, isUser: Boolean) {
    val context = textView.context ?: return
    val typedValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
    val arr =
        context.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.textColorPrimary))
    textView.setTextColor(
        if (isUser) {
            context.getColor(R.color.colorPrimary)
        } else {
            arr.getColor(0, -1)
        }
    )
    arr.recycle()
}