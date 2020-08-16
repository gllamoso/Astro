package dev.gtcl.reddit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.URLUtil
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.chip.Chip
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.databinding.ItemAwardBinding
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.ui.fragments.multireddits.MultiRedditSubredditsAdapter
import java.text.SimpleDateFormat
import java.util.*


@BindingAdapter("loadImageAndHideIfNull")
fun loadImageAndHideIfNull(imgView: ImageView, imgUrl: String?){
    if(imgUrl != null && URLUtil.isValidUrl(imgUrl) && Patterns.WEB_URL.matcher(imgUrl).matches()) {
        imgView.visibility = View.VISIBLE
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
//            .apply(RequestOptions.bitmapTransform(BlurTransformation()))
//            .apply(
//                RequestOptions()
//                    .placeholder(R.drawable.))
//                    .placeholder(R.color.background))
//                    .error(R.drawable.ic_broken_image_24))
            .into(imgView)
    }
    else {
        imgView.visibility = View.GONE
    }
}

@BindingAdapter("loadImage")
fun loadImage(imgView: ImageView, imgUrl: String?){
    if(imgUrl.isNullOrBlank()) return
    if(imgUrl.startsWith("http")){
        imgView.visibility = View.VISIBLE
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
//                .apply(
//                    RequestOptions()
//                        .placeholder(R.drawable.anim_loading)
//                        .error(R.drawable.ic_broken_image_24))
            .into(imgView)
    }
}

@BindingAdapter("account_banner")
fun loadAccountBanner(imgView: ImageView, url: String?){
    if (url.isNullOrBlank()){
        imgView.setImageResource(R.drawable.ic_sun_tornado)
    } else {
        Glide.with(imgView.context)
            .load(url)
            .into(imgView)
    }
}

@BindingAdapter("uri")
fun bindUriToImage(imgView: ImageView, uri: Uri?){
    if(uri == null){
        imgView.visibility = View.GONE
        return
    }

    imgView.visibility = View.VISIBLE
    Glide.with(imgView.context)
        .load(uri)
        .into(imgView)
}

@BindingAdapter("post")
fun bindVideo(playerView: PlayerView, post: Post?){
    if(post == null || post.postType != PostType.VIDEO){
        playerView.visibility = View.GONE
        return
    }
    playerView.visibility = View.VISIBLE
}

@BindingAdapter("post")
fun loadImage(subsamplingScaleImageView: SubsamplingScaleImageView, post: Post?){
    if(post == null || post.postType != PostType.IMAGE) {
        subsamplingScaleImageView.visibility = View.GONE
        return
    }
    subsamplingScaleImageView.visibility = View.VISIBLE
    loadImage(subsamplingScaleImageView, post.url)
}

@BindingAdapter("subsampleImage")
fun loadImage(subsamplingScaleImageView: SubsamplingScaleImageView, imgUrl: String?){
    Glide.with(subsamplingScaleImageView.context)
        .asBitmap()
        .load(imgUrl)
        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
        .into(SubsamplingScaleImageViewTarget(subsamplingScaleImageView))
}

class SubsamplingScaleImageViewTarget(view: SubsamplingScaleImageView): CustomViewTarget<SubsamplingScaleImageView, Bitmap>(view){
    override fun onLoadFailed(errorDrawable: Drawable?) {}

    override fun onResourceCleared(placeholder: Drawable?) {}

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        view.setImage(ImageSource.bitmap(resource))
    }
}

@BindingAdapter("listingType")
fun loadMultiIcon(imgView: ImageView, listing: Listing){
    when(listing){
        FrontPage -> imgView.setImageResource(R.drawable.ic_front_page_24)
        All -> imgView.setImageResource(R.drawable.ic_all_24)
        Popular -> imgView.setImageResource(R.drawable.ic_trending_up_24)
        is MultiRedditListing -> imgView.setImageResource(R.drawable.ic_collection_24)
        else -> imgView.setImageResource(R.drawable.ic_reddit_circle_24)
    }
}

@BindingAdapter("subredditIcon")
fun loadSubIcon(imgView: ImageView, imgUrl: String?){
    if(imgUrl == null || !imgUrl.startsWith("http")){
        imgView.setImageResource(R.drawable.ic_reddit_circle_24)
    }
    else {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
            .apply(RequestOptions()
                .placeholder(R.drawable.ic_reddit_circle_24)
                .circleCrop())
            .into(imgView)
    }
}

@BindingAdapter("accountIcon")
fun loadAccountIcon(imgView: ImageView, imgUrl: String?){
    if(imgUrl == null || !imgUrl.startsWith("http")){
        imgView.setImageResource(R.drawable.ic_profile_24)
    }
    else {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
            .apply(RequestOptions()
                .placeholder(R.drawable.ic_profile_24)
                .circleCrop())
            .into(imgView)
    }
}

@BindingAdapter("subscriptionIcon")
fun loadSubscriptionIcon(imgView: ImageView, subscription: Subscription){
    val placeHolder = when(subscription.type){
        SubscriptionType.MULTIREDDIT -> R.drawable.ic_collection_24
        SubscriptionType.USER -> R.drawable.ic_user_24
        SubscriptionType.SUBREDDIT -> R.drawable.ic_reddit_circle_24
    }

    if(subscription.icon == null || !subscription.icon.startsWith("https", true)){
        imgView.setImageResource(placeHolder)
        return
    }

    Glide.with(imgView.context)
        .load(subscription.icon)
        .apply(RequestOptions()
            .placeholder(placeHolder)
            .circleCrop())
        .into(imgView)
}

@BindingAdapter("multiRedditIcon")
fun loadMultiRedditIcon(imgView: ImageView, imgUrl: String?){
    if(imgUrl == null || !imgUrl.startsWith("http")){
        imgView.setImageResource(R.drawable.ic_collection_24)
    }
    else {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
            .apply(RequestOptions()
                .placeholder(R.drawable.ic_collection_24)
                .circleCrop())
            .into(imgView)
    }
}


@BindingAdapter("tint")
fun setTint(imgView: ImageView, color: Int){
    imgView.setColorFilter(color)
}

@BindingAdapter("favorite")
fun loadFavoriteIcon(imgView: ImageView, isFavorite: Boolean){
    imgView.setImageResource(if(isFavorite) R.drawable.ic_favorite_filled_24 else R.drawable.ic_favorite_unfilled_24)
}

@BindingAdapter("added")
fun loadAddedIcon(imgView: ImageView, added: Boolean){
    imgView.setImageResource(
        if(added){
            R.drawable.ic_remove_circle_outline_24
        } else {
            R.drawable.ic_add_circle_24
        }
    )
}

@BindingAdapter("listingType")
fun loadListingText(txtView: TextView, listing: Listing?){
    val context = txtView.context
    listing?.let {
        txtView.text = when(it){
            FrontPage -> context.getText(R.string.frontpage)
            All -> context.getText(R.string.all)
            Popular -> context.getText(R.string.popular_tab_label)
            is MultiRedditListing -> it.multiReddit.name
            is SubredditListing -> it.displayName
            is ProfileListing -> context.getText(when(it.info){
                ProfileInfo.OVERVIEW -> R.string.overview
                ProfileInfo.SUBMITTED -> R.string.submitted
                ProfileInfo.COMMENTS -> R.string.comments
                ProfileInfo.UPVOTED -> R.string.upvoted
                ProfileInfo.DOWNVOTED -> R.string.downvoted
                ProfileInfo.HIDDEN -> R.string.hidden
                ProfileInfo.SAVED -> R.string.saved
                ProfileInfo.GILDED -> R.string.gilded
            }
            )
            is SubscriptionListing -> it.subscription.name
        }
    }
}

@BindingAdapter("listingType")
fun setVisibility(viewGroup: ViewGroup, listing: Listing?) {
    if (listing == null) return
    viewGroup.visibility = if (listing is SubredditListing) View.VISIBLE else View.GONE
}

@BindingAdapter("indent")
fun setIndentation(linearLayout: LinearLayout, indent: Int){
    linearLayout.removeAllViews()
    val viewSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, .90F, linearLayout.context.resources.displayMetrics).toInt()
    val indentationSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8F, linearLayout.context.resources.displayMetrics).toInt()
    val params = LinearLayout.LayoutParams(viewSize, WindowManager.LayoutParams.MATCH_PARENT).apply {
        marginStart = indentationSize
    }
    for(i in 1..indent){
        val view = View(linearLayout.context).apply {
            setBackgroundColor(Color.GRAY)
            layoutParams = params
        }
        linearLayout.addView(view)
    }
}

@BindingAdapter("more_comment")
fun setMoreCommentText(textView: TextView, item: More){
    if(item.isContinueThreadLink)
        textView.text = textView.resources.getString(R.string.continue_to_thread)
    else
        textView.text = String.format(textView.resources.getText(R.string.more_replies).toString(), item.childrenLeft())
}

@BindingAdapter("timestamp")
fun setTimestamp(textView: TextView, time: Long?){
    if(time != null){
        val timestamp = timeSince(time)
        textView.text = timestamp
    } else {
        textView.text = ""
    }
}

@BindingAdapter("secondsToDate")
fun secondsToDate(textView: TextView, time: Long){
    val simpleDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ROOT)
    textView.text = simpleDateFormat.format(1000L * time)
}

@BindingAdapter("upvoteRatio")
fun setUpvoteRatio(textView: TextView, upvoteRatio: Double?){
    if(upvoteRatio != null){
        textView.text = String.format(textView.context.getString(R.string.upvote_ratio), upvoteRatio * 100)
    } else textView.text = ""
}

@BindingAdapter("setViewSize")
fun setViewSize(view: View, percentOfDeviceHeight: Int){
    val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val size = Point()
    display.getRealSize(size)
    val height = size.y * percentOfDeviceHeight / 100

    val layoutParams = view.layoutParams
    layoutParams.height = height
    view.layoutParams = layoutParams
}

@BindingAdapter("awards")
fun setAwardImages(layout: GridLayout, awards: List<Award>?){
    if(awards.isNullOrEmpty()) return
    for(award: Award in awards){
        val binding = ItemAwardBinding.inflate(LayoutInflater.from(layout.context))
        binding.award = award
        layout.addView(binding.root)
    }
}

@BindingAdapter("likes")
fun setViewColor(view: View, likes: Boolean?){
    when(likes){
        null -> view.setBackgroundColor(Color.TRANSPARENT)
        true -> view.setBackgroundColor(ContextCompat.getColor(view.context, android.R.color.holo_orange_dark))
        false -> view.setBackgroundColor(ContextCompat.getColor(view.context, android.R.color.holo_blue_dark))
    }
}

@BindingAdapter("read")
fun setTextColor(textView: TextView, isRead: Boolean?){
    if(isRead == true){
        textView.setTextColor(ContextCompat.getColor(textView.context, android.R.color.darker_gray))
    } else {
        textView.setTextColor(textView.context.resolveColorAttr(android.R.attr.textColorPrimary))
    }
}

@BindingAdapter("invert")
fun invert(view: View, inverted: Boolean?){
    if(inverted == null){
        view.visibility = View.GONE
    } else {
        view.visibility = View.VISIBLE
        view.rotation = if(inverted){
                180F
            } else {
                0F
            }
    }
}

@BindingAdapter("subredditsInMultireddit")
fun bindRecyclerViewForMultiReddit(recyclerView: RecyclerView, data: MutableList<Subreddit>){
    val adapter = recyclerView.adapter as MultiRedditSubredditsAdapter
    adapter.submitList(data)
}

@BindingAdapter("number_of_comments")
fun formatComments(textView: TextView, num: Int){
    val numFormatted = numFormatted(num.toLong())
    textView.text = String.format(textView.context.getText(R.string.num_comments).toString(), numFormatted)
}

@BindingAdapter("upvote_tint")
fun applyUpvoteTint(imageView: ImageView, likes: Boolean?){
    when(likes){
        true -> imageView.setColorFilter(ContextCompat.getColor(imageView.context, android.R.color.holo_orange_dark))
        else -> imageView.clearColorFilter()
    }
}

@BindingAdapter("downvote_tint")
fun applyDownvoteTint(imageView: ImageView, likes: Boolean?){
    when(likes){
        false -> imageView.setColorFilter(ContextCompat.getColor(imageView.context, android.R.color.holo_blue_dark))
        else -> imageView.clearColorFilter()
    }
}

@BindingAdapter("bookmark_tint")
fun applyBookmarkTint(imageView: ImageView, bookmarked: Boolean){
    when(bookmarked){
        true -> imageView.setColorFilter(ContextCompat.getColor(imageView.context, android.R.color.holo_orange_light))
        else -> imageView.clearColorFilter()
    }
}

@BindingAdapter("chip_is_checked")
fun checkChip(chip: Chip, check: Boolean){
    chip.isChecked = check
}