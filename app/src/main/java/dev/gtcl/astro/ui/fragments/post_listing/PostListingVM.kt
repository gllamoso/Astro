package dev.gtcl.astro.ui.fragments.post_listing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.R
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.getListingTitle
import dev.gtcl.astro.models.reddit.listing.MultiReddit
import dev.gtcl.astro.models.reddit.listing.PostListing
import dev.gtcl.astro.models.reddit.listing.Subreddit
import dev.gtcl.astro.ui.fragments.item_scroller.ItemScrollerVM
import kotlinx.coroutines.launch
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

    private val _sidebarError = MutableLiveData<String?>()
    val sidebarError: LiveData<String?>
        get() = _sidebarError

    override fun setListingInfo(postListing: PostListing, loadDefaultSorting: Boolean) {
        super.setListingInfo(postListing, loadDefaultSorting)
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
                _sidebarError.postValue(application.getString(R.string.something_went_wrong))
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
                val reddit = "reddit"
                for (subName in trendingSubredditNames) {
                    if(subName.equals(reddit, ignoreCase = true)){
                        subsListMutable.add(Subreddit(
                            reddit,
                            reddit,
                            "r/$reddit",
                            null,
                            reddit,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "",
                            "/r/$reddit",
                            null,
                            "public"))
                    } else {
                        subsListMutable.add(subredditRepository.getSubreddit(subName).await().data)
                    }
                }
                _trendingSubreddits.postValue(subsListMutable.toList())
            } catch (e: Exception) {
                Timber.tag(this@PostListingVM.javaClass.simpleName).e(e.toString())
                _sidebarError.postValue(application.getString(R.string.something_went_wrong))
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun fetchMultiReddit(path: String) {
        coroutineScope.launch {
            try {
                val multi = subredditRepository.getMultiReddit(path).await().data.apply {
                    parseDescription()
                }
                _multiReddit.postValue(multi)
            } catch (e: Exception) {
                Timber.tag(this@PostListingVM.javaClass.simpleName).e(e.toString())
                _sidebarError.postValue(application.getString(R.string.something_went_wrong))
                _errorMessage.postValue(e.getErrorMessage(application))
            }
        }
    }

    fun sidebarErrorObserved() {
        _sidebarError.value = null
    }

}