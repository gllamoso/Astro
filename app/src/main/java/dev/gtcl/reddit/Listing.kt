package dev.gtcl.reddit

import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import dev.gtcl.reddit.network.NetworkState

data class Listing<T>(
    val pagedList: LiveData<PagedList<T>>,
    val networkState: LiveData<NetworkState>,
    val refreshState: LiveData<NetworkState>,
    val refresh: () -> Unit,
    val retry: () -> Unit)