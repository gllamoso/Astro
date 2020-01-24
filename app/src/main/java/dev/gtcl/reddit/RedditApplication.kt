package dev.gtcl.reddit

import android.app.Application
import dev.gtcl.reddit.comments.CommentRepository
import dev.gtcl.reddit.comments.getCommentRepository
import dev.gtcl.reddit.posts.PostRepository
import dev.gtcl.reddit.posts.getPostRepository
import dev.gtcl.reddit.subs.SubredditRepository
import dev.gtcl.reddit.subs.getSubredditRepository
import dev.gtcl.reddit.users.UserRepository
import dev.gtcl.reddit.users.getUserRepository
import java.util.concurrent.Executors

class RedditApplication : Application() {

    val userRepository: UserRepository by lazy {
        getUserRepository(this)
    }

    val postRepository: PostRepository by lazy {
        getPostRepository(this, Executors.newFixedThreadPool(5))
    }

    val subredditRepository: SubredditRepository by lazy {
        getSubredditRepository(Executors.newFixedThreadPool(5))
    }

    val commentRepository: CommentRepository by lazy {
        getCommentRepository(this)
    }
}