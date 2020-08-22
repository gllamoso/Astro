package dev.gtcl.reddit.repositories

import android.util.Log
import androidx.annotation.MainThread
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.ItemRead
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

const val GUEST_ID = "guest"

class ListingRepository private constructor(private val application: RedditApplication){

    private val database = redditDatabase(application)

    // --- NETWORK
    @MainThread
    fun getListing(listing: Listing, postSort: PostSort, t: Time? = null, after: String?, pageSize: Int, user: String? = null): Deferred<ListingResponse>{
        val accessToken = application.accessToken
        val userName = user ?: application.currentAccount?.name
        return when(listing){
            FrontPage -> if(accessToken != null) {
                    RedditApi.oauth.getPostFromFrontPage(accessToken.authorizationHeader,
                        postSort, t, after, pageSize)
                } else {
                    RedditApi.base.getPostFromFrontPage(null, postSort, t, after, pageSize)
                }
            All -> if(accessToken != null) {
                    RedditApi.oauth.getPostsFromSubreddit(accessToken.authorizationHeader, "all",
                        postSort, t, after, pageSize)
                } else {
                    RedditApi.base.getPostsFromSubreddit(null, "all", postSort, t, after, pageSize)
                }
            Popular -> if(accessToken != null) {
                    RedditApi.oauth.getPostsFromSubreddit(accessToken.authorizationHeader, "popular", postSort, t, after, pageSize)
                } else {
                    RedditApi.base.getPostsFromSubreddit(null, "popular", postSort, t, after, pageSize)
                }
            is SearchListing -> if(accessToken != null){
                    RedditApi.oauth.searchPosts(accessToken.authorizationHeader, listing.query, postSort, t, after, pageSize)
                } else {
                    RedditApi.base.searchPosts(null, listing.query, postSort, t, after, pageSize)
                }
            is MultiRedditListing -> if(accessToken != null){
                    RedditApi.oauth.getMultiRedditListing(accessToken.authorizationHeader, listing.multiReddit.path.removePrefix("/"), postSort, t, after, pageSize)
                } else {
                    RedditApi.base.getMultiRedditListing(null , listing.multiReddit.path.removePrefix("/"), postSort, t, after, pageSize)
                }
            is SubredditListing -> if (accessToken != null) {
                    RedditApi.oauth.getPostsFromSubreddit(accessToken.authorizationHeader, listing.displayName, postSort, t, after, pageSize)
                } else {
                    RedditApi.base.getPostsFromSubreddit(null, listing.displayName, postSort, t, after, pageSize)
                }
            is ProfileListing -> if(accessToken != null){
                    RedditApi.oauth.getPostsFromUser(accessToken.authorizationHeader, userName!!, listing.info, after, pageSize)
                } else {
                RedditApi.base.getPostsFromUser(null, userName!!, listing.info, after, pageSize)
            }
            is SubscriptionListing -> {
                if(accessToken != null){
                    when(listing.subscription.type){
                        SubscriptionType.SUBREDDIT, SubscriptionType.USER -> RedditApi.oauth.getPostsFromSubreddit(accessToken.authorizationHeader, listing.subscription.name, postSort, t, after, pageSize)
                        SubscriptionType.MULTIREDDIT ->  RedditApi.oauth.getMultiRedditListing(accessToken.authorizationHeader, listing.subscription.url.removePrefix("/"), postSort, t, after, pageSize)
                    }
                } else {
                    when(listing.subscription.type){
                        SubscriptionType.SUBREDDIT, SubscriptionType.USER -> RedditApi.base.getPostsFromSubreddit(null, listing.subscription.name, postSort, t, after, pageSize)
                        SubscriptionType.MULTIREDDIT ->  RedditApi.base.getMultiRedditListing(null, listing.subscription.url.removePrefix("/"), postSort, t, after, pageSize)
                    }
                }
            }
        }
    }

    @MainThread
    fun vote(fullname: String, vote: Vote): Deferred<Response<Unit>> {
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.vote(application.accessToken!!.authorizationHeader, fullname, vote.value)
    }

    @MainThread
    fun save(id: String): Deferred<Response<Unit>>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.save(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun unsave(id: String): Deferred<Response<Unit>>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.unsave(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun hide(id: String): Deferred<Response<Unit>>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.hide(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun unhide(id: String): Deferred<Response<Unit>>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.unhide(application.accessToken!!.authorizationHeader, id)
    }


    @MainThread
    fun getAwards(user: String): Deferred<TrophyListingResponse>{
        return if(application.accessToken == null) RedditApi.base.getAwards(null, user)
            else RedditApi.oauth.getAwards(application.accessToken!!.authorizationHeader, user)
    }

    // --- DATABASE

    @MainThread
    suspend fun getReadPosts() = database.readItemDao.getAll()

    @MainThread
    suspend fun addReadItem(item: Item) {
        withContext(Dispatchers.IO){
            database.readItemDao.insert(ItemRead(item.name))
        }
    }

    // --- COMMENTS
    fun getPostAndComments(permalink: String, sort: CommentSort = CommentSort.BEST, limit: Int = 15): Deferred<CommentPage>{
        val linkWithoutDomain = permalink.replace("http[s]?://www\\.reddit\\.com/".toRegex(), "")
        return if(application.accessToken == null) {
            RedditApi.base.getPostAndComments(null, "$linkWithoutDomain.json", sort, limit)
        } else {
            RedditApi.oauth.getPostAndComments(application.accessToken!!.authorizationHeader, "$linkWithoutDomain.json", sort, limit)
        }
    }

    @MainThread
    fun getMoreComments(children: String, linkId: String, sort: CommentSort = CommentSort.BEST): Deferred<MoreChildrenResponse>{
        return if(application.accessToken == null) {
            RedditApi.base.getMoreComments(null, children, linkId, sort = sort)
        } else {
            RedditApi.oauth.getMoreComments(application.accessToken!!.authorizationHeader, children, linkId, sort = sort)
        }
    }

    @MainThread
    fun addComment(parentName: String, body: String): Deferred<MoreChildrenResponse>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.addComment(application.accessToken!!.authorizationHeader, parentName, body)
    }

    companion object{
        private lateinit var INSTANCE: ListingRepository
        fun getInstance(application: RedditApplication): ListingRepository {
            if(!Companion::INSTANCE.isInitialized){
                INSTANCE = ListingRepository(application)
            }
            return INSTANCE
        }
    }
}