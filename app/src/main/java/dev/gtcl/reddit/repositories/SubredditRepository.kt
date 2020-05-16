package dev.gtcl.reddit.repositories

import androidx.annotation.MainThread
import com.google.android.exoplayer2.util.Log
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubredditMineWhere
import dev.gtcl.reddit.SubredditWhere
import dev.gtcl.reddit.SubscribeAction
import dev.gtcl.reddit.database.DbSubreddit
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.models.reddit.ListingResponse
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.asSubredditDatabaseModels
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import java.lang.IllegalStateException

class SubredditRepository private constructor(private val application: RedditApplication){
    private val database = redditDatabase(application)

    @MainThread
    fun getNetworkSubreddits(where: SubredditWhere, after: String? = null, limit: Int = 100): Deferred<ListingResponse> {
        return if(application.accessToken != null) {
            RedditApi.oauth.getSubreddits(
                "bearer ${application.accessToken!!.value}",
                where,
                after,
                limit
            )
        } else {
            RedditApi.base.getSubreddits(null, where, after, limit)
        }
    }

    @MainThread
    fun getNetworkAccountSubreddits(limit: Int = 100, after: String? = null): Deferred<ListingResponse> {
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

    suspend fun removeSubreddit(sub: Subreddit){
        withContext(Dispatchers.IO){
            database.subredditDao.deleteSubreddit(application.currentAccount?.id ?: GUEST_ID, sub.displayName)
        }
    }

    @MainThread
    fun getSubscribedSubsLive() = database.subredditDao.getSubscribedSubsLive(application.currentAccount?.id ?: GUEST_ID)

    suspend fun getSubscribedSubs(displayName: String? = null): List<DbSubreddit>{
        return if(displayName == null)
            database.subredditDao.getSubscribedSubs(application.currentAccount?.id ?: GUEST_ID)
        else
            database.subredditDao.getSubscribedSubs(application.currentAccount?.id ?: GUEST_ID, displayName)
    }

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
    fun subscribe(srName: String, subscribeAction: SubscribeAction): Call<Void> {
        if(application.accessToken == null) throw IllegalStateException("User must be logged in to subscribe")
        return RedditApi.oauth.subscribeToSubreddit("bearer ${application.accessToken!!.value}", subscribeAction,  srName)
    }

    @MainThread
    fun searchSubreddits(nsfw: Boolean, includeProfiles: Boolean, limit: Int, query: String): Deferred<ListingResponse> {
        return if(application.accessToken == null) RedditApi.base.getSubredditNameSearch(null, nsfw, includeProfiles, limit, query)
        else RedditApi.oauth.getSubredditNameSearch("bearer ${application.accessToken!!.value}", nsfw, includeProfiles, limit, query)
    }

    companion object{
        private lateinit var INSTANCE: SubredditRepository
        fun getInstance(application: RedditApplication): SubredditRepository {
            if(!Companion::INSTANCE.isInitialized)
                INSTANCE = SubredditRepository(application)
            return INSTANCE
        }
    }
}