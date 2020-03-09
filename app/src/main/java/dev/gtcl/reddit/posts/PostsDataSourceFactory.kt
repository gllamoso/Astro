package dev.gtcl.reddit.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.users.AccessToken
import dev.gtcl.reddit.users.User
import java.util.concurrent.Executor

class PostsDataSourceFactory(private val accessToken: AccessToken?, private val user: User?, private val listingType: ListingType, private val sort: PostSort, private val t: Time?, private val retryExecutor: Executor) : DataSource.Factory<String, Post>() {
    private val _sourceLiveData = MutableLiveData<PageKeyedPostDataSource>()
    val sourceLiveData: LiveData<PageKeyedPostDataSource>
        get() = _sourceLiveData

    override fun create(): DataSource<String, Post> {
        val source = PageKeyedPostDataSource(accessToken, user, listingType, sort, t, retryExecutor)
        _sourceLiveData.postValue(source)
        return source
    }
}