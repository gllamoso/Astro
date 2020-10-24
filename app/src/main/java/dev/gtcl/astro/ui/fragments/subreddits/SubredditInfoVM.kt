package dev.gtcl.astro.ui.fragments.subreddits

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.listing.Subreddit
import kotlinx.coroutines.launch
import timber.log.Timber

class SubredditInfoVM(private val application: AstroApplication) : AstroViewModel(application){

    private val _subreddit = MutableLiveData<Subreddit?>()
    val subreddit: LiveData<Subreddit?>
        get() = _subreddit

    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val loading: LiveData<Boolean>
        get() = _loading

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    fun fetchSubreddit(displayName: String) {
        _title.value = displayName
        coroutineScope.launch {
            try {
                _loading.postValue(true)
                val sub = subredditRepository.getSubreddit(displayName)
                    .await().data
                sub.parseDescription()
                _subreddit.postValue(sub)
            } catch (e: Exception) {
                Timber.tag(this@SubredditInfoVM.javaClass.simpleName).e(e.toString())
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _loading.postValue(false)
            }
        }
    }
}