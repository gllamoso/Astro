package dev.gtcl.reddit.posts

import androidx.annotation.MainThread
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import dev.gtcl.reddit.Listing
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.database.ReadPost
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.users.AccessToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

class PostRepository internal constructor(application: RedditApplication, private val networkExecutor: Executor){
    private val database = redditDatabase(application)

    // --- NETWORK

    @MainThread
    fun getPostsOfSubreddit(accessToken: AccessToken?, listingType: ListingType, sort: PostSort, t: Time? = null, pageSize: Int) : Listing<Post> {

        val sourceFactory = PostsDataSourceFactory(accessToken, listingType, sort, t, networkExecutor)

        // We use toLiveData Kotlin extension function here, you could also use LivePagedListBuilder
        val livePagedList = sourceFactory.toLiveData(
            pageSize = pageSize,
            // provide custom executor for network requests, otherwise it will default to
            // Arch Components' IO pool which is also used for disk access
            fetchExecutor = networkExecutor)

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        return Listing(
            pagedList = livePagedList,
            networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                it.networkState
            },
            retry = {
                sourceFactory.sourceLiveData.value?.retryAllFailed()
            },
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }

    // --- DATABASE

    @MainThread
    fun getReadPostsFromDatabase() = database.readPostDao.getAll()

    @MainThread
    suspend fun insertReadPostToDatabase(readPost: ReadPost) {
        withContext(Dispatchers.IO){
            database.readPostDao.insert(readPost)
        }
    }
}

private lateinit var INSTANCE: PostRepository
fun getPostRepository(application: RedditApplication, networkExecutor: Executor): PostRepository{
    synchronized(PostRepository::class.java){
        if(!::INSTANCE.isInitialized)
            INSTANCE = PostRepository(application, networkExecutor)
    }
    return INSTANCE
}