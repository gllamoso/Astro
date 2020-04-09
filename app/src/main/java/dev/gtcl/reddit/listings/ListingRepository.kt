package dev.gtcl.reddit.listings

import androidx.annotation.MainThread
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.ItemsRead
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

const val GUEST_ID = "guest"

class ListingRepository private constructor(private val application: RedditApplication, private val networkExecutor: Executor){
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
    fun getReadPostsFromDatabase() = database.readItemDao.getAll()

    @MainThread
    suspend fun insertReadPostToDatabase(itemsRead: ItemsRead) {
        withContext(Dispatchers.IO){
            database.readItemDao.insert(itemsRead)
        }
    }

    // --- COMMENTS
    fun getPostAndComments(permalink: String, sort: CommentSort = CommentSort.BEST, limit: Int = 15): Deferred<CommentPage> =
        RedditApi.base.getPostAndComments(permalink = "$permalink.json", sort = sort, limit = limit)

    @MainThread
    fun getMoreComments(children: String, linkId: String, sort: CommentSort = CommentSort.BEST): Deferred<List<Child>> =
        RedditApi.base.getMoreComments(children = children, linkId = linkId, sort = sort)

//     ____  _  _  ____  ____
//    / ___)/ )( \(  _ \/ ___)
//    \___ \) \/ ( ) _ (\___ \
//    (____/\____/(____/(____/ // TODO: Create new Repo class

    @MainThread
    fun getSubreddits(where: SubredditWhere, after: String? = null, limit: Int = 100): Deferred<ListingResponse> {
        return if(application.accessToken != null)
            RedditApi.oauth.getSubreddits("bearer ${application.accessToken!!.value}", where, after, limit)
        else
            RedditApi.base.getSubreddits(null, where, after, limit)
    }

    @MainThread
    fun getAccountSubreddits(limit: Int = 100, after: String? = null): Deferred<ListingResponse> {
        return if(application.accessToken != null)
            RedditApi.oauth.getSubredditsOfMine("bearer ${application.accessToken!!.value}", SubredditMineWhere.SUBSCRIBER, after, limit)
        else
            RedditApi.base.getSubreddits(null, SubredditWhere.DEFAULT, after, limit)
    }

    @MainThread
    fun getSubsSearch(q: String, nsfw: String): Deferred<ListingResponse> = RedditApi.base.getSubredditsSearch(q, nsfw)

    suspend fun insertSubreddit(sub: Subreddit){
        withContext(Dispatchers.IO){
            database.subredditDao.insert(sub.asDbModel(application.currentAccount?.id ?: GUEST_ID))
        }
    }

    suspend fun insertSubreddits(subs: List<Subreddit>){
        withContext(Dispatchers.IO){
            database.subredditDao.insert(subs.asSubredditDatabaseModels(
                application.currentAccount?.id ?: GUEST_ID
            ))
        }
    }

    @MainThread
    fun getSubscribedSubsLive() = database.subredditDao.getSubscribedSubsLive(application.currentAccount?.id ?: GUEST_ID)

    suspend fun deleteSubscribedSubs() {
        if (application.currentAccount == null) return
        withContext(Dispatchers.IO) {
            database.subredditDao.deleteSubscribedSubs(application.currentAccount!!.id)
        }
    }


    @MainThread
    fun getNonFavoriteSubsLive() = database.subredditDao.getNonFavoriteSubsLive(application.currentAccount?.id ?: GUEST_ID)

    @MainThread
    fun getFavoriteSubsLive() = database.subredditDao.getFavoriteSubsLive(application.currentAccount?.id ?: GUEST_ID)

    suspend fun getFavoriteSubs() = database.subredditDao.getFavoriteSubs(application.currentAccount?.id ?: GUEST_ID)

    suspend fun addToFavorites(displayName: String, favorite: Boolean){
        if(application.currentAccount == null) return
        withContext(Dispatchers.IO){
            database.subredditDao.updateFavoriteSub(application.currentAccount!!.id, displayName, favorite)
        }
    }

    @MainThread
    fun subscribe(srName: String, subscribe: Boolean): Call<Void>{
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to subscribe")
        return RedditApi.oauth.subscribeToSubreddit("bearer ${application.accessToken!!.value}", if(subscribe) "sub" else "unsub",  srName)
    }

    companion object{
        private lateinit var INSTANCE: ListingRepository
        fun getInstance(application: RedditApplication, networkExecutor: Executor): ListingRepository{
            if(!::INSTANCE.isInitialized)
                INSTANCE = ListingRepository(application, networkExecutor)
            return INSTANCE
        }
    }
}