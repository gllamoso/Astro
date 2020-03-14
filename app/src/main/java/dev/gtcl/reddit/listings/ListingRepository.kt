package dev.gtcl.reddit.listings

import androidx.annotation.MainThread
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import dev.gtcl.reddit.Listing
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.database.ReadListing
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.network.ListingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

class PostRepository internal constructor(val application: RedditApplication, private val networkExecutor: Executor){
    private val database = redditDatabase(application)

    // --- NETWORK

    @MainThread
    fun getPostsFromNetwork(listingType: ListingType, sort: PostSort, t: Time? = null, pageSize: Int) : Listing<ListingItem> {

        val sourceFactory = ListingDataSourceFactory(application.accessToken, application.currentUser, listingType, sort, t, networkExecutor)

        // We use toLiveData Kotlin extension function here, you could also use LivePagedListBuilder
        val livePagedList = sourceFactory.toLiveData(
            pageSize = pageSize,
            // provide custom executor for network requests, otherwise it will default to
            // Arch Components' IO pool which is also used for disk access
            fetchExecutor = networkExecutor)

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveDataListing) {
            it.initialLoad
        }
        return Listing(
            pagedList = livePagedList,
            networkState = Transformations.switchMap(sourceFactory.sourceLiveDataListing) {
                it.networkState
            },
            retry = {
                sourceFactory.sourceLiveDataListing.value?.retryAllFailed()
            },
            refresh = {
                sourceFactory.sourceLiveDataListing.value?.invalidate()
            },
            refreshState = refreshState
        )
    }

    // --- DATABASE

    @MainThread
    fun getReadPostsFromDatabase() = database.readPostDao.getAll()

    @MainThread
    suspend fun insertReadPostToDatabase(readListing: ReadListing) {
        withContext(Dispatchers.IO){
            database.readPostDao.insert(readListing)
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