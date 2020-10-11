package dev.gtcl.astro.repositories.reddit

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

class ListingRepository private constructor(private val application: AstroApplication) {

    // --- NETWORK
    fun getPostListing(
        postListing: PostListing,
        postSort: PostSort,
        t: Time? = null,
        after: String?,
        pageSize: Int,
        count: Int
    ): Deferred<ListingResponse> {
        val accessToken = application.accessToken
        return when (postListing) {
            FrontPage -> if (accessToken != null) {
                RedditApi.oauth.getPostFromFrontPage(
                    accessToken.authorizationHeader,
                    postSort, t, after, count, pageSize
                )
            } else {
                RedditApi.base.getPostFromFrontPage(null, postSort, t, after, count, pageSize)
            }
            All -> if (accessToken != null) {
                RedditApi.oauth.getPostsFromSubreddit(
                    accessToken.authorizationHeader, "all",
                    postSort, t, after, count, pageSize
                )
            } else {
                RedditApi.base.getPostsFromSubreddit(
                    null,
                    "all",
                    postSort,
                    t,
                    after,
                    count,
                    pageSize
                )
            }
            Popular -> if (accessToken != null) {
                RedditApi.oauth.getPostsFromSubreddit(
                    accessToken.authorizationHeader,
                    "popular",
                    postSort,
                    t,
                    after,
                    count,
                    pageSize
                )
            } else {
                RedditApi.base.getPostsFromSubreddit(
                    null,
                    "popular",
                    postSort,
                    t,
                    after,
                    count,
                    pageSize
                )
            }
            is SearchListing -> if (accessToken != null) {
                RedditApi.oauth.searchPosts(
                    accessToken.authorizationHeader,
                    postListing.query,
                    postSort,
                    t,
                    after,
                    count,
                    pageSize
                )
            } else {
                RedditApi.base.searchPosts(null, postListing.query, postSort, t, after, count, pageSize)
            }
            is MultiRedditListing -> if (accessToken != null) {
                RedditApi.oauth.getMultiRedditListing(
                    accessToken.authorizationHeader,
                    postListing.path.removePrefix("/"),
                    postSort,
                    t,
                    after,
                    count,
                    pageSize
                )
            } else {
                RedditApi.base.getMultiRedditListing(
                    null,
                    postListing.path.removePrefix("/"),
                    postSort,
                    t,
                    after,
                    count,
                    pageSize
                )
            }
            is SubredditListing -> if (accessToken != null) {
                RedditApi.oauth.getPostsFromSubreddit(
                    accessToken.authorizationHeader,
                    postListing.displayName,
                    postSort,
                    t,
                    after,
                    count,
                    pageSize
                )
            } else {
                RedditApi.base.getPostsFromSubreddit(
                    null,
                    postListing.displayName,
                    postSort,
                    t,
                    after,
                    count,
                    pageSize
                )
            }
            is ProfileListing -> if (accessToken != null) {
                RedditApi.oauth.getPostsFromUser(
                    accessToken.authorizationHeader,
                    postListing.user ?: application.currentAccount?.name
                    ?: throw Exception("No user found"),
                    postListing.info,
                    after,
                    count,
                    pageSize
                )
            } else {
                RedditApi.base.getPostsFromUser(
                    null,
                    postListing.user ?: throw Exception("No user found"),
                    postListing.info,
                    after,
                    count,
                    pageSize
                )
            }
        }
    }

    fun getMessages(
        where: MessageWhere,
        after: String? = null,
        limit: Int? = null
    ): Deferred<ListingResponse> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.getMessages(
            application.accessToken!!.authorizationHeader,
            where,
            after,
            limit
        )
    }

    fun getSubredditsListing(
        where: SubredditWhere,
        after: String? = null,
        limit: Int = 100
    ): Deferred<ListingResponse> {
        return if (application.accessToken != null) {
            RedditApi.oauth.getSubreddits(
                application.accessToken!!.authorizationHeader,
                where,
                after,
                limit
            )
        } else {
            RedditApi.base.getSubreddits(null, where, after, limit)
        }
    }

    companion object {
        private lateinit var INSTANCE: ListingRepository
        fun getInstance(application: AstroApplication): ListingRepository {
            if (!Companion::INSTANCE.isInitialized) {
                INSTANCE = ListingRepository(application)
            }
            return INSTANCE
        }
    }
}