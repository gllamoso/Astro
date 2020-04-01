package dev.gtcl.reddit.listings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.Time
import dev.gtcl.reddit.listings.users.AccessToken
import dev.gtcl.reddit.listings.users.User
import java.util.concurrent.Executor

class ListingDataSourceFactory(private val accessToken: AccessToken?, private val user: User?, private val listingType: ListingType, private val sort: PostSort, private val t: Time?, private val retryExecutor: Executor) : DataSource.Factory<String, ListingItem>() {
    private val _sourceLiveData = MutableLiveData<ListingPageKeyedDataSource>()
    val sourceLiveDataListing: LiveData<ListingPageKeyedDataSource>
        get() = _sourceLiveData

    override fun create(): DataSource<String, ListingItem> {
        val source = ListingPageKeyedDataSource(accessToken, user, listingType, sort, t, retryExecutor)
        _sourceLiveData.postValue(source)
        return source
    }
}