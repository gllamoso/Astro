package dev.gtcl.reddit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dev.gtcl.reddit.databinding.ItemAwardBinding
import dev.gtcl.reddit.listings.*
import dev.gtcl.reddit.listings.Award
import dev.gtcl.reddit.listings.Comment
import dev.gtcl.reddit.listings.Item
import dev.gtcl.reddit.listings.More

@BindingAdapter("imageUrlAndHideIfNull")
fun bindImageAndHideIfNull(imgView: ImageView, imgUrl: String?){
    if(imgUrl == null) return
    if(imgUrl.startsWith("http")){
        imgView.visibility = View.VISIBLE
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
//            .apply(
//                RequestOptions()
//                    .placeholder(R.drawable.anim_loading)
//                    .error(R.drawable.ic_broken_image))
            .into(imgView)
    }
    else imgView.visibility = View.GONE
}

@BindingAdapter("imageUrl")
fun bindImage(imgView: ImageView, imgUrl: String?){
    if(imgUrl == null) return
    if(imgUrl.startsWith("http")){
        imgView.visibility = View.VISIBLE
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
//                .apply(
//                    RequestOptions()
//                        .placeholder(R.drawable.anim_loading)
//                        .error(R.drawable.ic_broken_image))
            .into(imgView)
    }
}

@BindingAdapter("listingType")
fun loadMultiIcon(imgView: ImageView, listingType: ListingType){
    when(listingType){
        FrontPage -> imgView.setImageResource(R.drawable.ic_front_page_24dp)
        All -> imgView.setImageResource(R.drawable.ic_all_24dp)
        Popular -> imgView.setImageResource(R.drawable.ic_trending_up_24dp)
        is MultiReddit -> imgView.setImageResource(R.drawable.ic_collection_24dp)
        is SubredditListing -> loadSubIcon(imgView, listingType.sub.iconImg)
        else -> imgView.setImageResource(R.drawable.ic_bookmark_24dp)
    }
}

@BindingAdapter("subredditIcon")
fun loadSubIcon(imgView: ImageView, imgUrl: String?){
    if(imgUrl == null || !imgUrl.startsWith("http")){
        imgView.setImageResource(R.drawable.ic_reddit_circle)
    }
    else {
        val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
        Glide.with(imgView.context)
            .load(imgUri)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.anim_loading)
                    .error(R.drawable.ic_broken_image))
            .into(imgView)
    }
}

@BindingAdapter("favorite")
fun loadFavoriteIcon(imgView: ImageView, isFavorite: Boolean){
    imgView.setImageResource(if(isFavorite) R.drawable.ic_favorite_filled_24dp else R.drawable.ic_favorite_unfilled_24dp)
}

@BindingAdapter("added")
fun loadAddedIcon(imgView: ImageView, added: Boolean){
    imgView.setImageResource(if(added) R.drawable.ic_remove_circle_outline_24dp else R.drawable.ic_add_circle_outline_24dp)
}

@BindingAdapter("listingType")
fun loadListingText(txtView: TextView, listingType: ListingType?){
    listingType?.let {
        txtView.text = when(it){
            FrontPage -> txtView.context.getText(R.string.frontpage)
            All -> txtView.context.getText(R.string.all)
            Popular -> txtView.context.getText(R.string.popular_tab_label)
            is MultiReddit -> it.name
            is SubredditListing -> it.sub.displayName
            is ProfileListing -> when(it.info){
                ProfileInfo.OVERVIEW -> txtView.context.getText(R.string.overview)
                ProfileInfo.SUBMITTED -> txtView.context.getText(R.string.submitted)
                ProfileInfo.COMMENTS -> txtView.context.getText(R.string.comments)
                ProfileInfo.UPVOTED -> txtView.context.getText(R.string.upvoted)
                ProfileInfo.DOWNVOTED -> txtView.context.getText(R.string.downvoted)
                ProfileInfo.HIDDEN -> txtView.context.getText(R.string.hidden)
                ProfileInfo.SAVED -> txtView.context.getText(R.string.saved)
                ProfileInfo.GILDED -> txtView.context.getText(R.string.gilded)
            }
        }
    }
}

@BindingAdapter("setVisibility")
fun setVisibility(view: View, constraint: Boolean) {
    view.visibility = if(constraint) View.VISIBLE else View.GONE
}

@BindingAdapter("commentItem")
fun setIndentation(view: View, listItem: Item?){
    listItem?.let {
        if (it.depth == 0) {
            view.visibility = View.GONE
            return
        }
        view.visibility = View.VISIBLE

        val indicatorSize = view.context.resources.getDimension(R.dimen.comment_indicator_size)
        val lp = LinearLayout.LayoutParams(indicatorSize.toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
        val leftMargin = 1.5 * indicatorSize * (listItem.depth - 1)
        lp.setMargins(leftMargin.toInt(), 0, 0, 0)
        view.layoutParams = lp

        when(listItem.depth % 5){
            0 -> view.setBackgroundColor(Color.BLUE)
            1 -> view.setBackgroundColor(Color.RED)
            2 -> view.setBackgroundColor(Color.GREEN)
            3 -> view.setBackgroundColor(Color.YELLOW)
            4 -> view.setBackgroundColor(Color.MAGENTA)
        }
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("comment")
fun setCommentInfo(view: LinearLayout, comment: Comment){
    view.removeAllViews()
    val authorTextView = TextView(view.context)
    authorTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.toFloat())
    authorTextView.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD)
    authorTextView.text = comment.author
    view.addView(authorTextView)
    if(comment.score != Int.MIN_VALUE && comment.author != "[deleted]"){
        val scoreTextView = TextView(view.context)
        scoreTextView.text = " • ${String.format(view.resources.getString(R.string.num_points), comment.score)}"
        scoreTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.toFloat())
        view.addView(scoreTextView)
    }
    if(comment.author != "[deleted]"){
        val timeTextView = TextView(view.context)
        timeTextView.text = " • ${timeSince(view.context, comment.created)}"
        timeTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.toFloat())
        view.addView(timeTextView)
    }
}

@BindingAdapter("moreComment")
fun setMoreCommentText(textView: TextView, item: More){
    if(item.isContinueThreadLink())
        textView.text = textView.resources.getString(R.string.continue_to_thread)
    else
        textView.text = String.format(textView.resources.getText(R.string.more_replies).toString(), item.count)
}

@BindingAdapter("timestamp")
fun setTimestamp(textView: TextView, time: Long?){
    if(time != null){
        val timestamp = timeSince(textView.context, time)
        textView.text = timestamp
    } else textView.text = ""
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