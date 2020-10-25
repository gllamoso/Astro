package dev.gtcl.astro.repositories.reddit

import dev.gtcl.astro.*
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.database.redditDatabase
import dev.gtcl.astro.models.reddit.ErrorResponse
import dev.gtcl.astro.models.reddit.NewPostResponse
import dev.gtcl.astro.models.reddit.RulesResponse
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.RedditApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class SubredditRepository private constructor(private val application: AstroApplication) {
    private val database = redditDatabase(application)

    fun getMySubreddits(limit: Int = 100, after: String? = null): Deferred<ListingResponse> {
        return if (application.accessToken != null) {
            RedditApi.oauth.getSubredditsOfMine(
                application.accessToken!!.authorizationHeader,
                SubredditMineWhere.SUBSCRIBER,
                after,
                limit
            )
        } else {
            RedditApi.base.getSubreddits(null, SubredditWhere.DEFAULT, after, limit)
        }
    }

    fun subscribe(subreddit: Subreddit, subscribe: Boolean): Deferred<Response<Unit>> =
        subscribe(subreddit.displayName, subscribe)

    fun searchSubreddits(
        nsfw: Boolean,
        includeProfiles: Boolean,
        limit: Int,
        query: String
    ): Deferred<ListingResponse> {
        return if (application.accessToken == null) {
            RedditApi.base.getSubredditNameSearch(null, nsfw, includeProfiles, limit, query)
        } else {
            RedditApi.oauth.getSubredditNameSearch(
                application.accessToken!!.authorizationHeader,
                nsfw,
                includeProfiles,
                limit,
                query
            )
        }
    }

    fun getSubreddit(displayName: String): Deferred<SubredditChild> {
        return if (application.accessToken == null) {
            RedditApi.base.getSubredditInfo(null, displayName)
        } else {
            RedditApi.oauth.getSubredditInfo(
                application.accessToken!!.authorizationHeader,
                displayName
            )
        }
    }

    fun getRules(displayName: String): Deferred<RulesResponse> {
        return if (application.accessToken == null) {
            RedditApi.base.getSubredditRules(null, displayName)
        } else {
            RedditApi.oauth.getSubredditRules(
                application.accessToken!!.authorizationHeader,
                displayName
            )
        }
    }

    fun getModeratedSubs(username: String): Deferred<ModeratedList> {
        return if (application.accessToken == null) {
            RedditApi.base.getModeratedSubs(
                null,
                username
            )
        } else {
            RedditApi.oauth.getModeratedSubs(
                application.accessToken!!.authorizationHeader,
                username
            )
        }
    }

    fun getTrendingSubreddits() = RedditApi.base.getTrendingSubredditNames()

    //                                       _
//        /\                            | |
//       /  \   ___ ___ ___  _   _ _ __ | |_ ___
//      / /\ \ / __/ __/ _ \| | | | '_ \| __/ __|
//     / ____ \ (_| (_| (_) | |_| | | | | |_\__ \
//    /_/    \_\___\___\___/ \__,_|_| |_|\__|___/
//
    fun subscribe(account: Account, subscribe: Boolean): Deferred<Response<Unit>> =
        subscribe("u_${account.name}", subscribe)

//     __  __       _ _   _        _____          _     _ _ _
//    |  \/  |     | | | (_)      |  __ \        | |   | (_) |
//    | \  / |_   _| | |_ _ ______| |__) |___  __| | __| |_| |_ ___
//    | |\/| | | | | | __| |______|  _  // _ \/ _` |/ _` | | __/ __|
//    | |  | | |_| | | |_| |      | | \ \  __/ (_| | (_| | | |_\__ \
//    |_|  |_|\__,_|_|\__|_|      |_|  \_\___|\__,_|\__,_|_|\__|___/
//

    fun getMyMultiReddits(): Deferred<List<MultiRedditChild>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.getMyMultiReddits(application.accessToken!!.authorizationHeader)
    }

    fun getMultiReddits(username: String): Deferred<List<MultiRedditChild>> {
        return if (application.accessToken == null) {
            RedditApi.base.getMultiReddits(null, username)
        } else {
            RedditApi.oauth.getMultiReddits(application.accessToken!!.authorizationHeader, username)
        }
    }

    fun getMultiReddit(multipath: String): Deferred<MultiRedditChild> {
        return if (application.accessToken == null) {
            RedditApi.base.getMultiReddit(null, multipath)
        } else {
            RedditApi.oauth.getMultiReddit(application.accessToken!!.authorizationHeader, multipath)
        }
    }

    fun deleteMultiReddit(multipath: String): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.deleteMultiReddit(
            application.accessToken!!.authorizationHeader,
            multipath
        )
    }

    fun deleteSubredditFromMultiReddit(
        multipath: String,
        subreddit: Subreddit
    ): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.deleteSubredditInMultiReddit(
            application.accessToken!!.authorizationHeader,
            multipath,
            subreddit.displayName
        )
    }

    fun updateMulti(multipath: String, model: MultiRedditUpdate): Deferred<MultiRedditChild> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.updateMulti(
            application.accessToken!!.authorizationHeader,
            multipath,
            model
        )
    }

    fun createMulti(model: MultiRedditUpdate): Deferred<MultiRedditChild> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.createMulti(application.accessToken!!.authorizationHeader, model)
    }

    fun copyMulti(
        multiPath: String,
        displayName: String,
        descriptionMd: String?
    ): Deferred<MultiRedditChild> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.copyMulti(
            application.accessToken!!.authorizationHeader,
            multiPath,
            displayName,
            descriptionMd
        )
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
    suspend fun insertSubreddits(subs: List<Subreddit>) {
        withContext(Dispatchers.IO) {
            database.subscriptionDao.insert(
                subs.asSubscriptions(
                    application.currentAccount?.id ?: GUEST_ID
                )
            )
        }
    }

    suspend fun insertSubreddit(sub: Subreddit) {
        withContext(Dispatchers.IO) {
            database.subscriptionDao.insert(
                sub.asSubscription(
                    application.currentAccount?.id ?: GUEST_ID
                )
            )
        }
    }

    suspend fun insertMultiReddits(multis: List<MultiReddit>) {
        withContext(Dispatchers.IO) {
            database.subscriptionDao.insert(multis.asSubscriptions())
        }
    }

    suspend fun insertMultiReddit(multi: MultiReddit) {
        withContext(Dispatchers.IO) {
            database.subscriptionDao.insert(multi.asSubscription())
        }
    }

    // UPDATE
    suspend fun addToFavorites(subscription: Subscription, favorite: Boolean) {
        withContext(Dispatchers.IO) {
            database.subscriptionDao.updateSubscription(subscription.id, favorite)
        }
    }

    fun subscribe(subscription: Subscription, subscribe: Boolean) =
        subscribe(subscription.name, subscribe)

    // DELETE
    suspend fun deleteAllMySubscriptions() {
        withContext(Dispatchers.IO) {
            database.subscriptionDao.deleteAllSubscriptions(
                application.currentAccount?.id ?: GUEST_ID
            )
        }
    }

    suspend fun deleteSubscription(subscription: Subscription) {
        withContext(Dispatchers.IO) {
            database.subscriptionDao.deleteSubscription(subscription.id)
        }
    }

    suspend fun deleteSubscription(subreddit: Subreddit) {
        deleteSubscription(subreddit.asSubscription(application.currentAccount?.id ?: GUEST_ID))
    }

    // GET
    suspend fun searchMySubscriptionsExcludingMultireddits(startsWith: String) =
        database.subscriptionDao.searchSubscriptionsExcludingMultiReddits(
            application.currentAccount?.id ?: GUEST_ID, "$startsWith%"
        )

    suspend fun getMySubscriptions(subscriptionType: SubscriptionType) =
        database.subscriptionDao.getSubscriptionsAlphabetically(
            application.currentAccount?.id ?: GUEST_ID, subscriptionType
        )

    suspend fun getMyFavoriteSubscriptions() =
        database.subscriptionDao.getFavoriteSubscriptionsAlphabetically(
            application.currentAccount?.id ?: GUEST_ID
        )

    suspend fun getMyFavoriteSubscriptions(subscriptionType: SubscriptionType) =
        database.subscriptionDao.getFavoriteSubscriptionsAlphabetically(
            application.currentAccount?.id ?: GUEST_ID, subscriptionType
        )

    suspend fun getMyFavoriteSubscriptionsExcludingMultireddits() =
        database.subscriptionDao.getFavoriteSubscriptionsAlphabeticallyExcluding(
            application.currentAccount?.id ?: GUEST_ID, SubscriptionType.MULTIREDDIT
        )

//     __  __ _
//    |  \/  (_)
//    | \  / |_ ___  ___
//    | |\/| | / __|/ __|
//    | |  | | \__ \ (__
//    |_|  |_|_|___/\___|

    fun subscribe(name: String, subscribe: Boolean): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }

        val action = if (subscribe) {
            SubscribeAction.SUBSCRIBE
        } else {
            SubscribeAction.UNSUBSCRIBE
        }

        return RedditApi.oauth.subscribe(
            application.accessToken!!.authorizationHeader,
            action,
            name
        )
    }

    fun getFlairs(srName: String): Deferred<List<Flair>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }

        return RedditApi.oauth.getFlairs(application.accessToken!!.authorizationHeader, srName)
    }

    fun submitTextPost(
        subreddit: String,
        title: String,
        text: String,
        nsfw: Boolean,
        spoiler: Boolean,
        flair: Flair?
    ): Deferred<NewPostResponse> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
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

    fun submitUrlPost(
        subreddit: String,
        title: String,
        url: String,
        nsfw: Boolean,
        spoiler: Boolean,
        flair: Flair?,
        resubmit: Boolean = false
    ): Deferred<NewPostResponse> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
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

    fun submitCrosspost(
        subreddit: String,
        title: String,
        nsfw: Boolean,
        spoiler: Boolean,
        flair: Flair?,
        crosspost: Post
    ): Deferred<NewPostResponse> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.submitPost(
            application.accessToken!!.authorizationHeader,
            subreddit,
            PostType.CROSSPOST,
            title,
            null,
            null,
            nsfw,
            spoiler,
            flair?.id,
            flair?.text,
            false,
            crosspostFullname = crosspost.name
        )
    }

    fun submitUrlPostForErrors(
        subreddit: String,
        title: String,
        url: String,
        nsfw: Boolean,
        spoiler: Boolean,
        flair: Flair?
    ): Deferred<ErrorResponse> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
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

    companion object {
        private lateinit var INSTANCE: SubredditRepository
        fun getInstance(application: AstroApplication): SubredditRepository {
            if (!Companion::INSTANCE.isInitialized) {
                INSTANCE = SubredditRepository(application)
            }
            return INSTANCE
        }
    }
}