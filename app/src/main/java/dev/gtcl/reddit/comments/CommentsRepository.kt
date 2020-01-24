package dev.gtcl.reddit.comments

import android.app.Application
import androidx.annotation.MainThread
import dev.gtcl.reddit.CommentSort
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.Deferred

class CommentRepository internal constructor(application: Application) {
    private val database = redditDatabase(application)

    @MainThread
    fun getPostAndComments(permalink: String, sort: CommentSort): Deferred<CommentPage> =
        RedditApi.retrofitServiceWithNoAuth.getPostAndComments(permalink = "$permalink.json", sort = sort.stringValue)

    @MainThread
    fun getComments(permalink: String, sort: CommentSort): Deferred<List<CommentItem>> =
        RedditApi.retrofitServiceWithNoAuth.getComments(permalink = "$permalink.json", sort = sort.stringValue)

    @MainThread
    fun getMoreComments(children: String, linkId: String, sort: CommentSort): Deferred<List<Child>> =
        RedditApi.retrofitServiceWithNoAuth.getMoreComments(children = children, linkId = linkId, sort = sort.stringValue)

}

private lateinit var INSTANCE: CommentRepository
fun getCommentRepository(application: Application): CommentRepository {
    synchronized(CommentRepository::class.java){
        if(!::INSTANCE.isInitialized)
            INSTANCE = CommentRepository(application)
    }
    return INSTANCE
}