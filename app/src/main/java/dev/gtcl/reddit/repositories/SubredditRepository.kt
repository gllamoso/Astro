package dev.gtcl.reddit.repositories

import androidx.annotation.MainThread
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.StringWriter
import kotlin.IllegalStateException

class SubredditRepository private constructor(private val application: RedditApplication){
    private val database = redditDatabase(application)

    @MainThread
    fun getSubredditsFromReddit(where: SubredditWhere, after: String? = null, limit: Int = 100): Deferred<ListingResponse> {
        return if(application.accessToken != null) {
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

    @MainThread
    fun getMySubredditsFromReddit(limit: Int = 100, after: String? = null): Deferred<ListingResponse> {
        return if(application.accessToken != null)
            RedditApi.oauth.getSubredditsOfMine(application.accessToken!!.authorizationHeader, SubredditMineWhere.SUBSCRIBER, after, limit)
        else
            RedditApi.base.getSubreddits(null, SubredditWhere.DEFAULT, after, limit)
    }

    @MainThread
    fun subscribe(srName: String, subscribeAction: SubscribeAction): Deferred<Response<Unit>> {
        if(application.accessToken == null) {
            throw IllegalStateException("User must be logged in to subscribe")
        }
        return RedditApi.oauth.subscribeToSubreddit(application.accessToken!!.authorizationHeader, subscribeAction, srName)
    }

    @MainThread
    fun searchSubredditsFromReddit(nsfw: Boolean, includeProfiles: Boolean, limit: Int, query: String): Deferred<ListingResponse> {
        return if(application.accessToken == null) RedditApi.base.getSubredditNameSearch(null, nsfw, includeProfiles, limit, query)
        else RedditApi.oauth.getSubredditNameSearch(application.accessToken!!.authorizationHeader, nsfw, includeProfiles, limit, query)
    }

//     __  __       _ _   _        _____          _     _ _ _
//    |  \/  |     | | | (_)      |  __ \        | |   | (_) |
//    | \  / |_   _| | |_ _ ______| |__) |___  __| | __| |_| |_ ___
//    | |\/| | | | | | __| |______|  _  // _ \/ _` |/ _` | | __/ __|
//    | |  | | |_| | | |_| |      | | \ \  __/ (_| | (_| | | |_\__ \
//    |_|  |_|\__,_|_|\__|_|      |_|  \_\___|\__,_|\__,_|_|\__|___/
//

    @MainThread
    fun getMyMultiRedditsFromReddit(): Deferred<List<MultiRedditChild>> {
        if(application.accessToken == null) {
            throw IllegalStateException("User must be logged in to fetch multireddits")
        }
        return RedditApi.oauth.getMyMultiReddits(application.accessToken!!.authorizationHeader)
    }

    @MainThread
    fun getMultiRedditFromReddit(multipath: String): Deferred<MultiRedditChild>{
        return if(application.accessToken == null) {
            RedditApi.base.getMultiReddit(null, multipath)
        } else {
            RedditApi.oauth.getMultiReddit(application.accessToken!!.authorizationHeader, multipath)
        }
    }

    @MainThread
    fun deleteMultiReddit(multipath: String): Deferred<Response<Unit>>{
        if(application.accessToken == null){
            throw IllegalStateException("User must be logged in to delete multireddit")
        }
        return RedditApi.oauth.deleteMultiReddit(application.accessToken!!.authorizationHeader, multipath)
    }

//      _____       _                   _       _   _
//     / ____|     | |                 (_)     | | (_)
//    | (___  _   _| |__  ___  ___ _ __ _ _ __ | |_ _  ___  _ __  ___
//     \___ \| | | | '_ \/ __|/ __| '__| | '_ \| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) \__ \ (__| |  | | |_) | |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|___/\___|_|  |_| .__/ \__|_|\___/|_| |_|___/
//                                       | |
//                                       |_|

    // INSERT
    @MainThread
    suspend fun insertSubreddits(subs: List<Subreddit>){
        withContext(Dispatchers.IO){
            database.subscriptionDao.insert(subs.asSubscriptions(application.currentAccount?.id ?: GUEST_ID))
        }
    }
    @MainThread
    suspend fun insertSubreddit(sub: Subreddit){
        withContext(Dispatchers.IO){
            database.subscriptionDao.insert(sub.asSubscription(application.currentAccount?.id ?: GUEST_ID))
        }
    }
    @MainThread
    suspend fun insertMultiReddits(multis: List<MultiReddit>){
        withContext(Dispatchers.IO){
            database.subscriptionDao.insert(multis.asSubscriptions())
        }
    }
    @MainThread
    suspend fun insertMultiReddit(multi: MultiReddit){
        withContext(Dispatchers.IO){
            database.subscriptionDao.insert(multi.asSubscription())
        }
    }

    // UPDATE
    suspend fun updateSubscription(name: String, favorite: Boolean){
        withContext(Dispatchers.IO){
            database.subscriptionDao.updateSubscription(application.currentAccount?.id ?: GUEST_ID, name, favorite)
        }
    }

    // DELETE
    @MainThread
    suspend fun deleteAllMySubscriptions() {
        withContext(Dispatchers.IO) {
            database.subscriptionDao.deleteAllSubscriptions(application.currentAccount?.id ?: GUEST_ID)
        }
    }
    @MainThread
    suspend fun deleteSubscription(name: String){
        withContext(Dispatchers.IO){
            database.subscriptionDao.deleteSubscription(application.currentAccount?.id ?: GUEST_ID, name)
        }
    }

    // GET
    suspend fun getMySubscription(name: String) = database.subscriptionDao.getSubscription(application.currentAccount?.id ?: GUEST_ID, name)
    suspend fun getMySubscriptions() = database.subscriptionDao.getSubscriptionsAlphabetically(application.currentAccount?.id ?: GUEST_ID)
    suspend fun getMySubscriptions(subscriptionType: SubscriptionType) = database.subscriptionDao.getSubscriptionsAlphabetically(application.currentAccount?.id ?: GUEST_ID, subscriptionType)
    suspend fun getMySubscriptionsExcludingMultireddits() = database.subscriptionDao.getSubscriptionsAlphabeticallyExcluding(application.currentAccount?.id ?: GUEST_ID, SubscriptionType.MULTIREDDIT)
    suspend fun getMyFavoriteSubscriptions() = database.subscriptionDao.getFavoriteSubscriptionsAlphabetically(application.currentAccount?.id ?: GUEST_ID)
    suspend fun getMyFavoriteSubscriptionsExcludingMultireddits() = database.subscriptionDao.getFavoriteSubscriptionsAlphabeticallyExcluding(application.currentAccount?.id ?: GUEST_ID, SubscriptionType.MULTIREDDIT)

    companion object{
        private lateinit var INSTANCE: SubredditRepository
        fun getInstance(application: RedditApplication): SubredditRepository {
            if(!Companion::INSTANCE.isInitialized)
                INSTANCE = SubredditRepository(application)
            return INSTANCE
        }
    }
}