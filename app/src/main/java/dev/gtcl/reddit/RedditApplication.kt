package dev.gtcl.reddit

import android.app.Application
import android.util.Log
import dev.gtcl.reddit.comments.CommentRepository
import dev.gtcl.reddit.comments.getCommentRepository
import dev.gtcl.reddit.listings.PostRepository
import dev.gtcl.reddit.listings.getPostRepository
import dev.gtcl.reddit.subs.SubredditRepository
import dev.gtcl.reddit.subs.getSubredditRepository
import dev.gtcl.reddit.users.AccessToken
import dev.gtcl.reddit.users.User
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

    var accessToken: AccessToken? = null
        set(value){
            field = value
            Log.d("TAE", "Access Token: ${value?.value}") // TODO: Remove
        }

    var currentUser: User? = null


}