package dev.gtcl.astro.repositories.reddit

import androidx.annotation.MainThread
import dev.gtcl.astro.*
import dev.gtcl.astro.database.ItemRead
import dev.gtcl.astro.database.redditDatabase
import dev.gtcl.astro.models.reddit.*
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.CommentPage
import dev.gtcl.astro.network.RedditApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val GUEST_ID = "guest"

class ListingRepository private constructor(private val application: AstroApplication){

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
    fun getMessages(where: MessageWhere, after: String? = null, limit: Int? = null): Deferred<ListingResponse>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.getMessages(application.accessToken!!.authorizationHeader, where, after, limit)
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

    companion object{
        private lateinit var INSTANCE: ListingRepository
        fun getInstance(application: AstroApplication): ListingRepository {
            if(!Companion::INSTANCE.isInitialized){
                INSTANCE = ListingRepository(application)
            }
            return INSTANCE
        }
    }
}