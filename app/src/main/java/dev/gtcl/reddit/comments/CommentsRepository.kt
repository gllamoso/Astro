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
    fun getPostAndComments(permalink: String, sort: CommentSort = CommentSort.BEST, limit: Int = 15): Deferred<CommentPage> =
        RedditApi.base.getPostAndComments(permalink = "$permalink.json", sort = sort.stringValue, limit = limit)

    @MainThread
    fun getMoreComments(children: String, linkId: String, sort: CommentSort = CommentSort.BEST): Deferred<List<Child>> =
        RedditApi.base.getMoreComments(children = children, linkId = linkId, sort = sort.stringValue)

}

// TODO: Redo singleton
private lateinit var INSTANCE: CommentRepository
fun getCommentRepository(application: Application): CommentRepository {
    synchronized(CommentRepository::class.java){
        if(!::INSTANCE.isInitialized)
            INSTANCE = CommentRepository(application)
    }
    return INSTANCE
}