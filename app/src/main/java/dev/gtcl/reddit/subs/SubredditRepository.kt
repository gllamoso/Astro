package dev.gtcl.reddit.subs

import androidx.annotation.MainThread
import androidx.lifecycle.Transformations
import androidx.paging.toLiveData
import dev.gtcl.reddit.Listing
import dev.gtcl.reddit.network.RedditApi
import dev.gtcl.reddit.users.AccessToken
import kotlinx.coroutines.Deferred
import java.util.concurrent.Executor

class SubredditRepository internal constructor(private val networkExecutor: Executor){

    @MainThread
    fun getSubs(where: String, accessToken: AccessToken? = null, limit: Int = 100, after: String? = null): Deferred<SubredditListingResponse> {
        return if(accessToken == null)
            RedditApi.base.getSubreddits(where = where, limit = limit)
        else
            RedditApi.oauth.getSubredditsOfMine(authorization = "bearer ${accessToken.value}", where = where, after = after, limit = limit)
    }

    @MainThread
    fun getSubsSearch(q: String, nsfw: String): Deferred<SubredditListingResponse>{
        return RedditApi.base.getSubredditsSearch(q, nsfw)
    }

    @MainThread
    fun getSubsListing(where: String, pageSize: Int) : Listing<Subreddit> {

        val sourceFactory = SubredditDataSourceFactory(
            where,
            networkExecutor
        )

        // We use toLiveData Kotlin extension function here, you could also use LivePagedListBuilder
        val livePagedList = sourceFactory.toLiveData(
            pageSize = pageSize,
            // provide custom executor for network requests, otherwise it will default to
            // Arch Components' IO pool which is also used for disk access
            fetchExecutor = networkExecutor)

        val refreshState = Transformations.switchMap(sourceFactory.sourceLiveData) {
            it.initialLoad
        }
        return Listing(
            pagedList = livePagedList,
            networkState = Transformations.switchMap(sourceFactory.sourceLiveData) {
                it.networkState
            },
            retry = {
                sourceFactory.sourceLiveData.value?.retryAllFailed()
            },
            refresh = {
                sourceFactory.sourceLiveData.value?.invalidate()
            },
            refreshState = refreshState
        )
    }
}

private lateinit var INSTANCE: SubredditRepository
fun getSubredditRepository(networkExecutor: Executor): SubredditRepository {
    synchronized(SubredditRepository::class.java) {
        if (!::INSTANCE.isInitialized)
            INSTANCE = SubredditRepository(networkExecutor)
    }
    return INSTANCE
}