package dev.gtcl.reddit.listings

import androidx.annotation.MainThread
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

class ListingRepository private constructor(val application: RedditApplication, private val networkExecutor: Executor){
    private val database = redditDatabase(application)

    // --- NETWORK
    @MainThread
    fun getListing(listingType: ListingType, sort: PostSort, t: Time? = null, after: String?, pageSize: Int, user: String? = null): Deferred<ListingResponse>{
        val accessToken = application.accessToken?.value
        val userName = user ?: application.currentAccount?.name
        return when(listingType){
            FrontPage -> if(accessToken != null) RedditApi.oauth.getPostFromFrontPage("bearer $accessToken", sort, t, after, pageSize)
                else RedditApi.base.getPostFromFrontPage(null, sort, t, after, pageSize)
            All -> if(accessToken != null) RedditApi.oauth.getPostsFromSubreddit("bearer $accessToken", "all", sort, t, after, pageSize)
                else RedditApi.base.getPostsFromSubreddit(null, "all", sort, t, after, pageSize)
            Popular -> if(accessToken != null) RedditApi.oauth.getPostsFromSubreddit("bearer $accessToken", "popular", sort, t, after, pageSize)
                else RedditApi.base.getPostsFromSubreddit(null, "popular", sort, t, after, pageSize)
            is MultiReddit -> TODO()
            is SubredditListing -> if (accessToken != null) RedditApi.oauth.getPostsFromSubreddit("bearer $accessToken", listingType.sub.displayName, sort, t, after, pageSize)
                else RedditApi.base.getPostsFromSubreddit(null, listingType.sub.displayName, sort, t, after, pageSize)
            is ProfileListing -> if(accessToken != null) RedditApi.oauth.getPostsFromUser("bearer $accessToken", userName!!, listingType.info, after, pageSize)
                else RedditApi.base.getPostsFromUser(null, userName!!, listingType.info, after, pageSize)
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
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to hide")
        return RedditApi.oauth.hide("bearer ${application.accessToken!!.value}", id)
    }

    @MainThread
    fun unhide(id: String): Call<Void>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to unhide")
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

    companion object{
        private lateinit var INSTANCE: ListingRepository
        fun getInstance(application: RedditApplication, networkExecutor: Executor): ListingRepository{
            if(!::INSTANCE.isInitialized)
                INSTANCE = ListingRepository(application, networkExecutor)
            return INSTANCE
        }
    }

//     ____  _  _  ____  ____
//    / ___)/ )( \(  _ \/ ___)
//    \___ \) \/ ( ) _ (\___ \
//    (____/\____/(____/(____/

    @MainThread
    fun getSubreddits(where: SubredditWhere, after: String? = null, limit: Int = 100): Deferred<ListingResponse> {
        return if(application.accessToken != null)
            RedditApi.oauth.getSubreddits("bearer ${application.accessToken!!.value}", where, after, limit)
        else
            RedditApi.base.getSubreddits(null, where, after, limit)
    }

    @MainThread
    fun getAccountSubreddit(limit: Int = 100, after: String? = null): Deferred<ListingResponse> {
        return if(application.accessToken != null)
            RedditApi.oauth.getSubredditsOfMine("bearer ${application.accessToken!!.value}", SubredditMineWhere.SUBSCRIBER, after, limit)
        else
            RedditApi.base.getSubreddits(null, SubredditWhere.DEFAULT, after, limit)
    }

    @MainThread
    fun getSubsSearch(q: String, nsfw: String): Deferred<ListingResponse> = RedditApi.base.getSubredditsSearch(q, nsfw)


}