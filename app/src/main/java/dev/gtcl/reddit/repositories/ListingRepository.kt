package dev.gtcl.reddit.repositories

import androidx.annotation.MainThread
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.ItemRead
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.models.reddit.Child
import dev.gtcl.reddit.models.reddit.CommentPage
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.lang.IllegalStateException

const val GUEST_ID = "guest"

class ListingRepository private constructor(private val application: RedditApplication){

    private val database = redditDatabase(application)

    // --- NETWORK
    @MainThread
    fun getListing(listingType: ListingType, sort: PostSort, t: Time? = null, after: String?, pageSize: Int, user: String? = null): Deferred<ListingResponse>{
        val accessToken = application.accessToken
        val userName = user ?: application.currentAccount?.name
        return when(listingType){
            FrontPage -> if(accessToken != null) {
                    RedditApi.oauth.getPostFromFrontPage(accessToken.authorizationHeader, sort, t, after, pageSize)
                } else {
                    RedditApi.base.getPostFromFrontPage(null, sort, t, after, pageSize)
                }
            All -> if(accessToken != null) {
                    RedditApi.oauth.getPostsFromSubreddit(accessToken.authorizationHeader, "all", sort, t, after, pageSize)
                } else {
                    RedditApi.base.getPostsFromSubreddit(null, "all", sort, t, after, pageSize)
                }
            Popular -> if(accessToken != null) {
                    RedditApi.oauth.getPostsFromSubreddit(accessToken.authorizationHeader, "popular", sort, t, after, pageSize)
                } else {
                    RedditApi.base.getPostsFromSubreddit(null, "popular", sort, t, after, pageSize)
                }
            is MultiRedditListing -> if(accessToken != null){
                    RedditApi.oauth.getMultiRedditListing(accessToken.authorizationHeader, listingType.multiReddit.path.removePrefix("/"), sort, t, after, pageSize)
                } else {
                    RedditApi.base.getMultiRedditListing(null , listingType.multiReddit.path.removePrefix("/"), sort, t, after, pageSize)
                }
            is SubredditListing -> if (accessToken != null) {
                    RedditApi.oauth.getPostsFromSubreddit(accessToken.authorizationHeader, listingType.sub.displayName, sort, t, after, pageSize)
                } else {
                    RedditApi.base.getPostsFromSubreddit(null, listingType.sub.displayName, sort, t, after, pageSize)
                }
            is ProfileListing -> if(accessToken != null){
                    RedditApi.oauth.getPostsFromUser(accessToken.authorizationHeader, userName!!, listingType.info, after, pageSize)
                } else {
                RedditApi.base.getPostsFromUser(null, userName!!, listingType.info, after, pageSize)
            }
            is SubscriptionListing -> {
                if(accessToken != null){
                    when(listingType.subscription.type){
                        SubscriptionType.SUBREDDIT, SubscriptionType.USER -> RedditApi.oauth.getPostsFromSubreddit(accessToken.authorizationHeader, listingType.subscription.name, sort, t, after, pageSize)
                        SubscriptionType.MULTIREDDIT ->  RedditApi.oauth.getMultiRedditListing(accessToken.authorizationHeader, listingType.subscription.url.removePrefix("/"), sort, t, after, pageSize)
                    }
                } else {
                    when(listingType.subscription.type){
                        SubscriptionType.SUBREDDIT, SubscriptionType.USER -> RedditApi.base.getPostsFromSubreddit(null, listingType.subscription.name, sort, t, after, pageSize)
                        SubscriptionType.MULTIREDDIT ->  RedditApi.base.getMultiRedditListing(null, listingType.subscription.url.removePrefix("/"), sort, t, after, pageSize)
                    }
                }
            }
        }
    }

    @MainThread
    fun vote(fullname: String, vote: Vote): Deferred<Response<Unit>> {
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to vote")
        return RedditApi.oauth.vote(application.accessToken!!.authorizationHeader, fullname, vote.value)
    }

    @MainThread
    fun save(id: String): Deferred<Response<Unit>>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to save")
        return RedditApi.oauth.save(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun unsave(id: String): Deferred<Response<Unit>>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to unsave")
        return RedditApi.oauth.unsave(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun hide(id: String): Deferred<Response<Unit>>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to hide")
        return RedditApi.oauth.hide(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun unhide(id: String): Deferred<Response<Unit>>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to unhide")
        return RedditApi.oauth.unhide(application.accessToken!!.authorizationHeader, id)
    }


    @MainThread
    fun getAwards(user: String): Deferred<TrophyListingResponse>{
        return if(application.accessToken == null) RedditApi.base.getAwards(null, user)
            else RedditApi.oauth.getAwards(application.accessToken!!.authorizationHeader, user)
    }

    // --- DATABASE

    @MainThread
    fun getReadPostsLiveData() = database.readItemDao.getAllLiveData()

    @MainThread
    suspend fun getReadPosts() = database.readItemDao.getAll()

    @MainThread
    suspend fun addReadItem(item: Item) {
        withContext(Dispatchers.IO){
            database.readItemDao.insert(ItemRead(item.name))
        }
    }

    // --- COMMENTS
    fun getPostAndComments(permalink: String, sort: CommentSort = CommentSort.BEST, limit: Int = 15): Deferred<CommentPage> =
        RedditApi.base.getPostAndComments(permalink = "$permalink.json", sort = sort, limit = limit)

    @MainThread
    fun getMoreComments(children: String, linkId: String, sort: CommentSort = CommentSort.BEST): Deferred<List<Child>> =
        RedditApi.base.getMoreComments(children = children, linkId = linkId, sort = sort)

    companion object{
        private lateinit var INSTANCE: ListingRepository
        fun getInstance(application: RedditApplication): ListingRepository {
            if(!Companion::INSTANCE.isInitialized)
                INSTANCE = ListingRepository(application)
            return INSTANCE
        }
    }
}