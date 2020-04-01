package dev.gtcl.reddit.listings

import androidx.annotation.MainThread
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.ReadListing
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.listings.comments.Child
import dev.gtcl.reddit.listings.comments.CommentPage
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import java.lang.IllegalStateException
import java.util.concurrent.Executor

class ListingRepository internal constructor(val application: RedditApplication, private val networkExecutor: Executor){
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
    fun getListing(listingType: ListingType, sort: PostSort, t: Time? = null, after: String?, pageSize: Int): Deferred<ListingResponse>{
        val accessToken = application.accessToken
        val user = application.currentUser
        return when(listingType){
            FrontPage -> if(accessToken != null) RedditApi.oauth.getPostFromFrontPage("bearer " + accessToken.value, sort, t, after, pageSize)
                else RedditApi.base.getPostFromFrontPage(null, sort, t, after, pageSize)
            All -> if(accessToken != null) RedditApi.oauth.getPostsFromSubreddit("bearer " + accessToken.value, "all", sort, t, after, pageSize)
                else RedditApi.base.getPostsFromSubreddit(null, "all", sort, t, after, pageSize)
            Popular -> if(accessToken != null) RedditApi.oauth.getPostsFromSubreddit("bearer " + accessToken.value, "popular", sort, t, after, pageSize)
                else RedditApi.base.getPostsFromSubreddit(null, "popular", sort, t, after, pageSize)
            is MultiReddit -> TODO()
            is SubredditListing -> if (accessToken != null) RedditApi.oauth.getPostsFromSubreddit("bearer " + accessToken.value, listingType.sub.displayName, sort, t, after, pageSize)
                else RedditApi.base.getPostsFromSubreddit(null, listingType.sub.displayName, sort, t, after, pageSize)
            is ProfileListing -> if(accessToken != null && user != null) RedditApi.oauth.getPostsFromUser("bearer "  + accessToken.value, user.name, listingType.info, after, pageSize)
                else RedditApi.base.getPostsFromUser(null, user!!.name, listingType.info, after, pageSize)
        }
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
    fun hide(id: String): Call<Void>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to unsave")
        return RedditApi.oauth.hide("bearer ${application.accessToken!!.value}", id)
    }

    @MainThread
    fun unhide(id: String): Call<Void>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to unsave")
        return RedditApi.oauth.unhide("bearer ${application.accessToken!!.value}", id)
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

    // --- COMMENTS
    @MainThread
    fun getPostAndComments(permalink: String, sort: CommentSort = CommentSort.BEST, limit: Int = 15): Deferred<CommentPage> =
        RedditApi.base.getPostAndComments(permalink = "$permalink.json", sort = sort, limit = limit)

    @MainThread
    fun getMoreComments(children: String, linkId: String, sort: CommentSort = CommentSort.BEST): Deferred<List<Child>> =
        RedditApi.base.getMoreComments(children = children, linkId = linkId, sort = sort)
}

private lateinit var INSTANCE: ListingRepository
fun getPostRepository(application: RedditApplication, networkExecutor: Executor): ListingRepository{
    synchronized(ListingRepository::class.java){
        if(!::INSTANCE.isInitialized)
            INSTANCE = ListingRepository(application, networkExecutor)
    }
    return INSTANCE
}