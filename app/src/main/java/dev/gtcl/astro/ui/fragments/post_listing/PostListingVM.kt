package dev.gtcl.astro.ui.fragments.post_listing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.fragments.item_scroller.ItemScrollerVM
import kotlinx.coroutines.*
import timber.log.Timber

class PostListingVM(val application: AstroApplication) : ItemScrollerVM(application) {

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private val _subreddit = MutableLiveData<Subreddit?>().apply { value = null }
    val subreddit: LiveData<Subreddit?>
        get() = _subreddit

    private val _bannerImg = MutableLiveData<String?>()
    val bannerImg: LiveData<String?>
        get() = _bannerImg

    private val _trendingSubreddits = MutableLiveData<List<Subreddit>?>()
    val trendingSubreddits: LiveData<List<Subreddit>?>
        get() = _trendingSubreddits

    private val _multiReddit = MutableLiveData<MultiReddit?>()
    val multiReddit: LiveData<MultiReddit?>
        get() = _multiReddit

    override fun setListingInfo(postListing: PostListing) {
        super.setListingInfo(postListing)
        _title.value = getListingTitle(application, postListing)
    }

    fun fetchSubreddit(displayName: String) {
        coroutineScope.launch {
            try {
                val sub = subredditRepository.getSubreddit(displayName)
                    .await().data
                _subreddit.postValue(sub)
                _bannerImg.postValue(sub.banner)
            } catch (e: Exception) {
                Timber.tag(this@PostListingVM.javaClass.simpleName).e(e.toString())
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun fetchTrendingSubreddits() {
        coroutineScope.launch {
            try {
                val trendingSubredditNames =
                    subredditRepository.getTrendingSubreddits().await().subredditNames
                val subsListMutable = mutableListOf<Subreddit>()
                for (subName in trendingSubredditNames) {
                    subsListMutable.add(subredditRepository.getSubreddit(subName).await().data)
                }
                _trendingSubreddits.postValue(subsListMutable.toList())
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun fetchMultiReddit(path: String) {
        coroutineScope.launch {
            try {
                _multiReddit.postValue(subredditRepository.getMultiReddit(path).await().data)
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

}