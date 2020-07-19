package dev.gtcl.reddit.repositories

import android.util.Log
import androidx.annotation.MainThread
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.models.reddit.ErrorResponse
import dev.gtcl.reddit.models.reddit.PostSubmittedResponse
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import java.net.URL
import kotlin.IllegalStateException

class SubredditRepository private constructor(private val application: RedditApplication){
    private val database = redditDatabase(application)

    @MainThread
    fun getSubredditsListing(where: SubredditWhere, after: String? = null, limit: Int = 100): Deferred<ListingResponse> {
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
    fun getMySubreddits(limit: Int = 100, after: String? = null): Deferred<ListingResponse> {
        return if(application.accessToken != null) {
            RedditApi.oauth.getSubredditsOfMine(application.accessToken!!.authorizationHeader, SubredditMineWhere.SUBSCRIBER, after, limit)
        }
        else {
            RedditApi.base.getSubreddits(null, SubredditWhere.DEFAULT, after, limit)
        }
    }

    @MainThread
    fun subscribe(subreddit: Subreddit, subscribe: Boolean): Deferred<Response<Unit>> = subscribe(subreddit.displayName, subscribe)

    @MainThread
    fun searchSubreddits(nsfw: Boolean, includeProfiles: Boolean, limit: Int, query: String): Deferred<ListingResponse> {
        return if(application.accessToken == null) {
            RedditApi.base.getSubredditNameSearch(null, nsfw, includeProfiles, limit, query)
        } else {
            RedditApi.oauth.getSubredditNameSearch(application.accessToken!!.authorizationHeader, nsfw, includeProfiles, limit, query)
        }
    }

    @MainThread
    fun getSubreddit(displayName: String): Deferred<SubredditChild>{
        return if(application.accessToken == null){
            RedditApi.base.getSubredditInfo(null, displayName)
        } else {
            RedditApi.oauth.getSubredditInfo(application.accessToken!!.authorizationHeader, displayName)
        }
    }

    @MainThread
    fun getRules(displayName: String): Deferred<RulesResponse>{
        return if(application.accessToken == null){
            RedditApi.base.getSubredditRules(null, displayName)
        } else {
            RedditApi.oauth.getSubredditRules(application.accessToken!!.authorizationHeader, displayName)
        }
    }

//                                       _
//        /\                            | |
//       /  \   ___ ___ ___  _   _ _ __ | |_ ___
//      / /\ \ / __/ __/ _ \| | | | '_ \| __/ __|
//     / ____ \ (_| (_| (_) | |_| | | | | |_\__ \
//    /_/    \_\___\___\___/ \__,_|_| |_|\__|___/
//
    @MainThread
    fun subscribe(account: Account, subscribe: Boolean): Deferred<Response<Unit>> = subscribe("u_${account.name}", subscribe)

//     __  __       _ _   _        _____          _     _ _ _
//    |  \/  |     | | | (_)      |  __ \        | |   | (_) |
//    | \  / |_   _| | |_ _ ______| |__) |___  __| | __| |_| |_ ___
//    | |\/| | | | | | __| |______|  _  // _ \/ _` |/ _` | | __/ __|
//    | |  | | |_| | | |_| |      | | \ \  __/ (_| | (_| | | |_\__ \
//    |_|  |_|\__,_|_|\__|_|      |_|  \_\___|\__,_|\__,_|_|\__|___/
//

    @MainThread
    fun getMyMultiReddits(): Deferred<List<MultiRedditChild>> {
        if(application.accessToken == null) {
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }
        return RedditApi.oauth.getMyMultiReddits(application.accessToken!!.authorizationHeader)
    }

    @MainThread
    fun getMultiReddit(multipath: String): Deferred<MultiRedditChild>{
        return if(application.accessToken == null) {
            RedditApi.base.getMultiReddit(null, multipath)
        } else {
            RedditApi.oauth.getMultiReddit(application.accessToken!!.authorizationHeader, multipath)
        }
    }

    @MainThread
    fun deleteMultiReddit(multipath: String): Deferred<Response<Unit>>{
        if(application.accessToken == null){
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }
        return RedditApi.oauth.deleteMultiReddit(application.accessToken!!.authorizationHeader, multipath)
    }

    @MainThread
    fun deleteSubredditFromMultiReddit(multipath: String, subreddit: Subreddit): Deferred<Response<Unit>>{
        if(application.accessToken == null){
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }
        return RedditApi.oauth.deleteSubredditInMultiReddit(application.accessToken!!.authorizationHeader, multipath, subreddit.displayName)
    }

    @MainThread
    fun createOrUpdateMultiReddit(multipath: String, model: MultiRedditUpdate): Deferred<MultiRedditChild> {
        if(application.accessToken == null){
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }
        return RedditApi.oauth.createOrUpdateMultiReddit(application.accessToken!!.authorizationHeader, multipath, model)
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
    @MainThread
    suspend fun insertAccount(account: Account){
        withContext(Dispatchers.IO){
            database.subscriptionDao.insert(account.asSubscription(application.currentAccount?.id ?: GUEST_ID))
        }
    }

    // UPDATE
    suspend fun addToFavorites(subscription: Subscription, favorite: Boolean){
        withContext(Dispatchers.IO){
            database.subscriptionDao.updateSubscription(subscription.id, favorite)
        }
    }

    @MainThread
    fun subscribe(subscription: Subscription, subscribe: Boolean) = subscribe(subscription.name, subscribe)

    // DELETE
    @MainThread
    suspend fun deleteAllMySubscriptions() {
        withContext(Dispatchers.IO) {
            database.subscriptionDao.deleteAllSubscriptions(application.currentAccount?.id ?: GUEST_ID)
        }
    }
    @MainThread
    suspend fun deleteSubscription(subscription: Subscription){
        withContext(Dispatchers.IO){
            database.subscriptionDao.deleteSubscription(subscription.id)
        }
    }
    @MainThread
    suspend fun deleteSubscription(subreddit: Subreddit){
        deleteSubscription(subreddit.asSubscription(application.currentAccount?.id ?: GUEST_ID))
    }
    @MainThread
    suspend fun deleteSubscription(account: Account){
        deleteSubscription(account.asSubscription(application.currentAccount?.id ?: GUEST_ID))
    }

    // GET
    suspend fun searchMySubscriptionsExcludingMultireddits(startsWith: String) = database.subscriptionDao.searchSubscriptionsExcludingMultiReddits(application.currentAccount?.id ?: GUEST_ID, "$startsWith%")
    suspend fun getMySubscription(subredditName: String) = database.subscriptionDao.getSubscription("${subredditName}__${application.currentAccount?.id ?: GUEST_ID}")
    suspend fun getMySubscriptions() = database.subscriptionDao.getSubscriptionsAlphabetically(application.currentAccount?.id ?: GUEST_ID)
    suspend fun getMySubscriptions(subscriptionType: SubscriptionType) = database.subscriptionDao.getSubscriptionsAlphabetically(application.currentAccount?.id ?: GUEST_ID, subscriptionType)
    suspend fun getMySubscriptionsExcludingMultireddits() = database.subscriptionDao.getSubscriptionsAlphabeticallyExcluding(application.currentAccount?.id ?: GUEST_ID, SubscriptionType.MULTIREDDIT)
    suspend fun getMyFavoriteSubscriptions() = database.subscriptionDao.getFavoriteSubscriptionsAlphabetically(application.currentAccount?.id ?: GUEST_ID)
    suspend fun getMyFavoriteSubscriptions(subscriptionType: SubscriptionType) = database.subscriptionDao.getFavoriteSubscriptionsAlphabetically(application.currentAccount?.id ?: GUEST_ID, subscriptionType)
    suspend fun getMyFavoriteSubscriptionsExcludingMultireddits() = database.subscriptionDao.getFavoriteSubscriptionsAlphabeticallyExcluding(application.currentAccount?.id ?: GUEST_ID, SubscriptionType.MULTIREDDIT)

//     __  __ _
//    |  \/  (_)
//    | \  / |_ ___  ___
//    | |\/| | / __|/ __|
//    | |  | | \__ \ (__
//    |_|  |_|_|___/\___|

    @MainThread
    fun subscribe(name: String, subscribe: Boolean): Deferred<Response<Unit>> {
        if(application.accessToken == null){
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }

        val action = if(subscribe){
            SubscribeAction.SUBSCRIBE
        } else {
            SubscribeAction.UNSUBSCRIBE
        }

        return RedditApi.oauth.subscribe(application.accessToken!!.authorizationHeader, action, name)
    }

    @MainThread
    fun getFlairs(srName: String): Deferred<List<Flair>>{
        if(application.accessToken == null){
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }

        return RedditApi.oauth.getFlairs(application.accessToken!!.authorizationHeader, srName)
    }

    @MainThread
    fun submitTextPost(
        subreddit: String,
        title: String,
        text: String,
        nsfw: Boolean,
        spoiler: Boolean,
        flair: Flair?
    ): Deferred<PostSubmittedResponse>{
        if(application.accessToken == null){
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }
        return RedditApi.oauth.submitPost(
            application.accessToken!!.authorizationHeader,
            subreddit,
            PostType.TEXT,
            title,
            text,
            null,
            nsfw,
            spoiler,
            flair?.id,
            flair?.text,
            true
        )
    }

    @MainThread
    fun submitUrlPost(
        subreddit: String,
        title: String,
        url: String,
        nsfw: Boolean,
        spoiler: Boolean,
        flair: Flair?,
        resubmit: Boolean = false
    ): Deferred<PostSubmittedResponse>{
        if(application.accessToken == null){
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }
        return RedditApi.oauth.submitPost(
            application.accessToken!!.authorizationHeader,
            subreddit,
            PostType.URL,
            title,
            null,
            url,
            nsfw,
            spoiler,
            flair?.id,
            flair?.text,
            resubmit
        )
    }

    @MainThread
    fun submitUrlPostForErrors(
        subreddit: String,
        title: String,
        url: String,
        nsfw: Boolean,
        spoiler: Boolean,
        flair: Flair?
    ): Deferred<ErrorResponse>{
        if(application.accessToken == null){
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }
        return RedditApi.oauth.submitPostForError(
            application.accessToken!!.authorizationHeader,
            subreddit,
            PostType.URL,
            title,
            null,
            url,
            nsfw,
            spoiler,
            flair?.id,
            flair?.text
        )
    }

    @MainThread
    fun sendRepliesToInbox(id: String, state: Boolean): Deferred<Response<Unit>>{
        if(application.accessToken == null){
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }
        return RedditApi.oauth.setSendReplies(
            application.accessToken!!.authorizationHeader,
            id,
            state
        )
    }

    companion object{
        private lateinit var INSTANCE: SubredditRepository
        fun getInstance(application: RedditApplication): SubredditRepository {
            if(!Companion::INSTANCE.isInitialized){
                INSTANCE = SubredditRepository(application)
            }
            return INSTANCE
        }
    }
}