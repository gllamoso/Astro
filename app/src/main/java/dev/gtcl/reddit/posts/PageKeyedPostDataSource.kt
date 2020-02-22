package dev.gtcl.reddit.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.Executor

class PageKeyedPostDataSource(private val subredditName: String, private val sort: PostSort, private val t: Time?, private val retryExecutor: Executor) : PageKeyedDataSource<String, Post>() {

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

    override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<String, Post>) {
        // ignored, since we only ever append to our initial load
    }


    override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String, Post>) {
        dataSourceScope.launch {
            _networkState.postValue(NetworkState.LOADING)
            val resultsFromRepo = RedditApi.retrofitServiceWithNoAuth.getPostsFromSubreddit(
                subreddit = subredditName,
                sort = sort.stringValue,
                t = t?.stringValue,
                after = params.key,
                limit = params.requestedLoadSize)
            try {
                val data = resultsFromRepo.await().data
                val items = data.children.map { it.data }
                retry = null
                callback.onResult(items, data.after)
                _networkState.postValue(NetworkState.LOADED)
            } catch (e: Exception) {
                retry = { loadAfter(params, callback) }
                _networkState.postValue(NetworkState.error("Exception: ${e.message}"))
            }
        }

    }

    override fun loadInitial(params: LoadInitialParams<String>, callback: LoadInitialCallback<String, Post>) {
        dataSourceScope.launch {
            _networkState.postValue(NetworkState.LOADING)
            _initialLoad.postValue(NetworkState.LOADING)
            val request = RedditApi.retrofitServiceWithNoAuth.getPostsFromSubreddit(subreddit = subredditName, sort = sort.stringValue, t = t?.stringValue, limit = params.requestedLoadSize)

            // triggered by a refresh, we better execute sync
            try {
                val data = request.await().data
                val items = data.children.map { it.data }
                retry = null
                _networkState.postValue(NetworkState.LOADED)
                _initialLoad.postValue(NetworkState.LOADED)
                callback.onResult(items, data.before, data.after)
            } catch (ioException: IOException) {
                retry = { loadInitial(params, callback) }
                val error = NetworkState.error(ioException.message ?: "unknown error")
                _networkState.postValue(error)
                _initialLoad.postValue(error)
            }
        }
    }

}