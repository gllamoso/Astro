package dev.gtcl.reddit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import androidx.paging.PagedList
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dev.gtcl.reddit.comments.CommentItem
import dev.gtcl.reddit.comments.More
import dev.gtcl.reddit.posts.RedditPost
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.ui.fragments.posts.PostListAdapter
import dev.gtcl.reddit.ui.fragments.posts.subreddits.mine.SubredditsListAdapter
import dev.gtcl.reddit.ui.fragments.posts.subreddits.popular.SubredditsPageListAdapter
import dev.gtcl.reddit.ui.fragments.posts.subreddits.trending.TrendingAdapter
import java.math.BigDecimal
import java.math.RoundingMode

@BindingAdapter("imageUrl")
fun bindImage(imgView: ImageView, imgUrl: String?){
    imgUrl?.let {
        if(it.startsWith("http")){
            imgView.visibility = View.VISIBLE
            val imgUri = imgUrl.toUri().buildUpon().scheme("https").build()
            Glide.with(imgView.context)
                .load(imgUri)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.anim_loading)
                        .error(R.drawable.ic_broken_image))
                .into(imgView)
        }
        else imgView.visibility = View.GONE
    }
}

@BindingAdapter("redditIcon")
fun loadIcon(imgView: ImageView, imgUrl: String?){
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

@BindingAdapter("setVisibility")
fun setVisibility(view: View, constraint: Boolean) {
    view.visibility = if(constraint) View.VISIBLE else View.GONE
}

@BindingAdapter("posts")
fun setPosts(recyclerView: RecyclerView, posts: PagedList<RedditPost>?){
    recyclerView.adapter?.let {
        (it as PostListAdapter).submitList(posts)
    }
}

@BindingAdapter("commentItem")
fun setIndentation(view: View, listItem: CommentItem?){
    listItem?.let {
        if(listItem.depth == 0) {
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

@BindingAdapter("moreComment")
fun setMoreCommentTextView(textView: TextView, more: More){
    if(more.isContinueThreadLink())
        textView.text = textView.resources.getString(R.string.continue_to_thread)
    else
        textView.text = String.format(textView.resources.getText(R.string.more_replies).toString(), more.count)
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

@SuppressLint("SetTextI18n")
@BindingAdapter("score")
fun setScore(textView: TextView, score: Int?){
    if(score != null){
        if(score >= 1000)
            textView.text = "${BigDecimal(score.toDouble()/1000).setScale(1, RoundingMode.HALF_EVEN)}K"
        else
            textView.text = score.toString()
    }
    else
        textView.text = "•"
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
