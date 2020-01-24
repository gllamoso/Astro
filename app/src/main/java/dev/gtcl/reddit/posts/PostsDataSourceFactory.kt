package dev.gtcl.reddit.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.Time
import java.util.concurrent.Executor

class PostsDataSourceFactory(private val subredditName: String, private val sort: PostSort, private val t: Time?, private val retryExecutor: Executor) : DataSource.Factory<String, RedditPost>() {
    private val _sourceLiveData = MutableLiveData<PageKeyedPostDataSource>()
    val sourceLiveData: LiveData<PageKeyedPostDataSource>
        get() = _sourceLiveData

    override fun create(): DataSource<String, RedditPost> {
        val source = PageKeyedPostDataSource(
            subredditName,
            sort,
            t,
            retryExecutor
        )
        _sourceLiveData.postValue(source)
        return source
    }
}