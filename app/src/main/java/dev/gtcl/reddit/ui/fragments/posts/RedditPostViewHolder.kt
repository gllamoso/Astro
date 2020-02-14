package dev.gtcl.reddit.ui.fragments.posts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.ItemRedditPostBinding
import dev.gtcl.reddit.posts.RedditPost

//class RedditPostViewHolder(view: View, private val glide: GlideRequests)
//class RedditPostViewHolder(view: View)
//    : RecyclerView.ViewHolder(view) {
//    private val title: TextView = view.findViewById(R.id.title)
//    private val subtitle: TextView = view.findViewById(R.id.subtitle)
//    private val score: TextView = view.findViewById(R.id.score)
//    private val thumbnail : ImageView = view.findViewById(R.id.thumbnail)
//    private var post : RedditPost? = null
//    init {
//        view.setOnClickListener {
//            post?.url?.let { url ->
//                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
//                view.context.startActivity(intent)
//            }
//        }
//    }
//
//    fun bind(post: RedditPost?) {
//        this.post = post
//        title.text = post?.title ?: "loading"
//        subtitle.text = itemView.context.resources.getString(R.string.post_subtitle,
//            post?.author ?: "unknown")
//        score.text = "${post?.score ?: 0}"
//        if (post?.thumbnail?.startsWith("http") == true) {
//            thumbnail.visibility = View.VISIBLE
////            glide.load(post.thumbnail)
////                .centerCrop()
////                .placeholder(R.drawable.ic_insert_photo_black_48dp)
////                .into(thumbnail)
//        } else {
//            thumbnail.visibility = View.GONE
////            glide.clear(thumbnail)
//        }
//    }
//
//    companion object {
////        fun create(parent: ViewGroup, glide: GlideRequests): RedditPostViewHolder {
//            fun create(parent: ViewGroup): RedditPostViewHolder {
//            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_reddit_post, parent, false)
////            return RedditPostViewHolder(view, glide)
//            return RedditPostViewHolder(view)
//        }
//    }
//
//    fun updateScore(item: RedditPost?) {
//        post = item
//        score.text = "${item?.score ?: 0}"
//    }
//}

class RedditPostViewHolder private constructor(private val binding:ItemRedditPostBinding)
    : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: RedditPost?, postClickListener: PostClickListener, isRead: Boolean, position: Int){
        binding.post = post
        binding.executePendingBindings()
        setIfRead(isRead)
        binding.root.setOnClickListener {
            setIfRead(true)
            postClickListener.onPostClick(post, position)
        }
    }

    private fun setIfRead(isRead: Boolean){
        binding.title.setTextColor(ContextCompat.getColor(binding.root.context, if(isRead) android.R.color.darker_gray else R.color.textColor))
    }



//    init {
//        view.setOnClickListener {
//            post?.url?.let { url ->
//                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
//                view.context.startActivity(intent)
//            }
//        }
//    }

    companion object {
        fun create(parent: ViewGroup): RedditPostViewHolder {
            return RedditPostViewHolder(ItemRedditPostBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }

//    fun updateScore(item: RedditPost?) {
//        post = item
//        score.text = "${item?.score ?: 0}"
//    }
}

interface PostClickListener {
    fun onPostClick(redditPost: RedditPost?, position: Int)
}
