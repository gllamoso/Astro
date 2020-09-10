package dev.gtcl.astro.ui.fragments.listing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.ui.fragments.item_scroller.ItemScrollerVM
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.collections.HashSet

class ListingVM(val application: AstroApplication) : ItemScrollerVM(application) {

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private val _subreddit = MutableLiveData<Subreddit?>()
    val subreddit: LiveData<Subreddit?>
        get() = _subreddit

    override fun setListingInfo(listing: Listing) {
        super.setListingInfo(listing)
        _title.value = getListingTitle(application, listing)
    }

    fun fetchSubreddit(displayName: String) {
        coroutineScope.launch {
            try {
                val sub = subredditRepository.getSubreddit(displayName)
                    .await().data
                _subreddit.postValue(sub)
            } catch (e: Exception) {
                Timber.tag(this@ListingVM.javaClass.simpleName).e(e.toString())
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun setSubreddit(sub: Subreddit?) {
        _subreddit.value = sub
    }

}