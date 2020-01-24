package dev.gtcl.reddit.subs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import dev.gtcl.reddit.subs.PageKeyedSubredditDataSource
import dev.gtcl.reddit.subs.Subreddit
import java.util.concurrent.Executor

class SubredditDataSourceFactory internal constructor(private val where: String, private val retryExecutor: Executor) : DataSource.Factory<String, Subreddit>() {
    private val _sourceLiveData = MutableLiveData<PageKeyedSubredditDataSource>()
    val sourceLiveData: LiveData<PageKeyedSubredditDataSource>
        get() = _sourceLiveData

    override fun create(): DataSource<String, Subreddit> {
        val source = PageKeyedSubredditDataSource(
            where,
            retryExecutor
        )
        _sourceLiveData.postValue(source)
        return source
    }
}