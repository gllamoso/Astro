package dev.gtcl.reddit

import android.graphics.Color
import android.view.View
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

@BindingAdapter("subredditsAsList")
fun setSubredditPosts(recyclerView: RecyclerView, subs: List<Subreddit>?){
    recyclerView.adapter?.let {
        (it as SubredditsListAdapter).submitList(subs)
    }
}

@BindingAdapter("subredditsAsPagedList")
fun setSubredditPagedList(recyclerView: RecyclerView, subs: PagedList<Subreddit>?){
    recyclerView.adapter?.let {
        (it as SubredditsPageListAdapter).submitList(subs)
    }
}

@BindingAdapter("trendingSubreddits")
fun setTrendingSubreddits(recyclerView: RecyclerView, posts: PagedList<RedditPost>?){
    recyclerView.adapter?.let {
        (it as TrendingAdapter).submitList(posts)
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
        val leftMargin = 3 * indicatorSize * (listItem.depth)
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

@BindingAdapter("authorAndTimestamp")
fun setAuthorAndTimestampTextView(textView: TextView, post: RedditPost?){
    post?.let {
        val authorAndTimestamp = textView.resources.getString(R.string.author_and_timestamp)
        val time = timeSince(textView.context, it.created)
        textView.text = String.format(authorAndTimestamp, it.author, time)
    }
}
