package dev.gtcl.reddit.listings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.network.*
import dev.gtcl.reddit.listings.users.AccessToken
import dev.gtcl.reddit.listings.users.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InvalidObjectException
import java.util.concurrent.Executor

class ListingPageKeyedDataSource(
    private val accessToken: AccessToken?,
    private  val user: User?,
    private val listingType: ListingType,
    private val sort: PostSort,
    private val t: Time?,
    private val retryExecutor: Executor) : PageKeyedDataSource<String, ListingItem>()
{

    private val dataSourceJob = Job()
    private val dataSourceScope = CoroutineScope(
        dataSourceJob + Dispatchers.Main)

    // keep a function reference for the retry event
    private var retry: (() -> Any)? = null

    /**
     * There is no sync on the state because paging will always call loadInitial first then wait
     * for it to return some success stringValue before calling loadAfter.
     */
    private val _networkState = MutableLiveData<NetworkState>()
    val networkState: LiveData<NetworkState>
        get() = _networkState

    private val _initialLoad = MutableLiveData<NetworkState>()
    val initialLoad: LiveData<NetworkState>
        get() = _initialLoad

    fun retryAllFailed() {
        val prevRetry = retry
        retry = null
        prevRetry?.let {
            retryExecutor.execute {
                it.invoke()
            }
        }
    }

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<String, ListingItem>) {}

    override fun loadInitial(params: LoadInitialParams<String>, callback: LoadInitialCallback<String, ListingItem>) {
        dataSourceScope.launch {
            _networkState.postValue(NetworkState.LOADING)
            _initialLoad.postValue(NetworkState.LOADING)
            val request = when(listingType){
                FrontPage -> if(accessToken != null) RedditApi.oauth.getPostFromFrontPage("bearer " + accessToken.value, sort, t, limit = params.requestedLoadSize)
                    else RedditApi.base.getPostFromFrontPage(null, sort, t, limit = params.requestedLoadSize)
                All -> if(accessToken != null) RedditApi.oauth.getPostsFromSubreddit(authorization = "bearer " + accessToken.value, subreddit = "all", sort = sort, t = t, limit = params.requestedLoadSize)
                    else RedditApi.base.getPostsFromSubreddit(null, subreddit = "all", sort = sort, t = t, limit = params.requestedLoadSize)
                Popular -> if(accessToken != null) RedditApi.oauth.getPostsFromSubreddit(authorization = "bearer " + accessToken.value, subreddit = "popular", sort = sort, t = t, limit = params.requestedLoadSize)
                    else RedditApi.base.getPostsFromSubreddit(null, subreddit = "popular", sort = sort, t = t, limit = params.requestedLoadSize)
                is MultiReddit -> TODO()
                is SubredditListing -> if (accessToken != null) RedditApi.oauth.getPostsFromSubreddit(authorization = "bearer " + accessToken.value, subreddit = listingType.sub.displayName, sort = sort, t = t, limit = params.requestedLoadSize)
                    else RedditApi.base.getPostsFromSubreddit(null, subreddit = listingType.sub.displayName, sort = sort, t = t, limit = params.requestedLoadSize)
                is ProfileListing -> if(accessToken != null && user != null) RedditApi.oauth.getPostsFromUser("bearer "  + accessToken.value, user.name, listingType.info, null, params.requestedLoadSize)
                    else RedditApi.base.getPostsFromUser(null, user!!.name, listingType.info, null, params.requestedLoadSize)
            }

            // triggered by a refresh, we better execute sync
            try {
                val data = request.await().data
                val items = data.children.map {
                    when (it) {
                        is PostListing -> it.data
                        is CommentListing -> it.data
                        is MoreListing -> it.data
                        else -> throw InvalidObjectException("Did not object in listing: ${it.kind}")
                    }
                }
                retry = null
                _networkState.postValue(NetworkState.LOADED)
                _initialLoad.postValue(NetworkState.LOADED)
                callback.onResult(items, data.before, data.after)
            } catch (ioException: IOException) {
                retry = { loadInitial(params, callback) }
                val error = NetworkState.error(ioException.message ?: "Unknown error")
                _networkState.postValue(error)
                _initialLoad.postValue(error)
            }
        }
    }


    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String, ListingItem>) {
        dataSourceScope.launch {
            _networkState.postValue(NetworkState.LOADING)
            val resultsFromRepo = when(listingType){
                FrontPage -> if(accessToken != null ) RedditApi.oauth.getPostFromFrontPage("bearer " + accessToken.value, sort, t, params.key, params.requestedLoadSize)
                    else RedditApi.base.getPostFromFrontPage(null, sort, t, params.key, params.requestedLoadSize)
                All -> if(accessToken != null) RedditApi.oauth.getPostsFromSubreddit(authorization = "bearer " + accessToken.value, subreddit = "all", sort = sort, t = t, after = params.key, limit = params.requestedLoadSize)
                    else RedditApi.base.getPostsFromSubreddit(null, subreddit = "all", sort = sort, t = t, after = params.key, limit = params.requestedLoadSize)
                Popular -> if(accessToken != null) RedditApi.oauth.getPostsFromSubreddit(authorization = "bearer " + accessToken.value, subreddit = "popular", sort = sort, t = t, after = params.key, limit = params.requestedLoadSize)
                    else RedditApi.base.getPostsFromSubreddit(null, subreddit = "popular", sort = sort, t = t, after = params.key, limit = params.requestedLoadSize)
                is MultiReddit -> TODO()
                is SubredditListing -> if(accessToken != null) RedditApi.oauth.getPostsFromSubreddit(authorization = "bearer" + accessToken.value, subreddit = listingType.sub.displayName, sort = sort, t = t, after = params.key, limit = params.requestedLoadSize)
                    else RedditApi.base.getPostsFromSubreddit(null, subreddit = listingType.sub.displayName, sort = sort, t = t, after = params.key, limit = params.requestedLoadSize)
                is ProfileListing -> if(accessToken != null && user != null) RedditApi.oauth.getPostsFromUser("bearer "  + accessToken.value, user.name, listingType.info, params.key, params.requestedLoadSize)
                    else RedditApi.base.getPostsFromUser(null, user!!.name, listingType.info, params.key, params.requestedLoadSize)
            }
            try {
                val data = resultsFromRepo.await().data
                val items = data.children.map {
                    when (it) {
                        is PostListing -> it.data
                        is CommentListing -> it.data
                        is MoreListing -> it.data
                        else -> throw InvalidObjectException("Did not object in listing: ${it.kind}")
                    }
                }
                retry = null
                callback.onResult(items, data.after)
                _networkState.postValue(NetworkState.LOADED)
            } catch (e: Exception) {
                retry = { loadAfter(params, callback) }
                _networkState.postValue(NetworkState.error("Exception: ${e.message}"))
            }
        }

    }



}