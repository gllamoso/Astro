package dev.gtcl.reddit.listings

import androidx.annotation.MainThread
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.ReadListing
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.network.ListingItem
import dev.gtcl.reddit.network.RedditApi
import dev.gtcl.reddit.network.TrophyListingResponse
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import java.lang.IllegalStateException
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

    @MainThread
    fun vote(fullname: String, vote: Vote): Call<Void> {
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to vote")
        return RedditApi.oauth.vote("bearer ${application.accessToken!!.value}", fullname, vote.value)
    }

    @MainThread
    fun save(id: String): Call<Void>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to save")
        return RedditApi.oauth.save("bearer ${application.accessToken!!.value}", id)
    }

    @MainThread
    fun unsave(id: String): Call<Void>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to unsave")
        return RedditApi.oauth.unsave("bearer ${application.accessToken!!.value}", id)
    }

    @MainThread
    fun getAwards(user: String): Deferred<TrophyListingResponse>{
        return if(application.accessToken == null) RedditApi.base.getAwards(null, user)
            else RedditApi.oauth.getAwards("bearer ${application.accessToken!!.value}", user)
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