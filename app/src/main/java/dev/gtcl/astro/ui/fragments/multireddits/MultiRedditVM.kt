package dev.gtcl.astro.ui.fragments.multireddits

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.listing.MultiReddit
import dev.gtcl.astro.models.reddit.listing.MultiRedditUpdate
import dev.gtcl.astro.models.reddit.listing.Subreddit
import dev.gtcl.astro.models.reddit.listing.SubredditData
import kotlinx.coroutines.*
import retrofit2.HttpException

class MultiRedditVM(private val application: AstroApplication) : AstroViewModel(application) {

    private val _subreddits =
        MutableLiveData<MutableList<Subreddit>>().apply { value = mutableListOf() }
    val subreddits: LiveData<MutableList<Subreddit>>
        get() = _subreddits

    private val _multi = MutableLiveData<MultiReddit>()
    val multi: LiveData<MultiReddit>
        get() = _multi

    private lateinit var multipath: String

    private var _initialized: Boolean = false
    val initialized: Boolean
        get() = _initialized

    private val _isLoading = MutableLiveData<Boolean>().apply { value = true }
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    fun fetchMultiReddit(subscription: Subscription) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                multipath = subscription.url.replaceFirst("/", "")
                val response = subredditRepository.getMultiReddit(multipath).await()
                _multi.postValue(response.data)
                _subreddits.postValue(response.data.subreddits.map { it.data!! }.toMutableList())
                _initialized = true
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun remove(subreddit: Subreddit, position: Int) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                _subreddits.value?.removeAt(position)
                _subreddits.postValue(_subreddits.value ?: mutableListOf())
                val response =
                    subredditRepository.deleteSubredditFromMultiReddit(multipath, subreddit).await()
                if (response.code() != 200) {
                    throw HttpException(response)
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun addSubredditsToMultiReddit(names: List<String>) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                val currentList: MutableList<Subreddit> = if (initialized) {
                    _subreddits.value ?: mutableListOf()
                } else {
                    subredditRepository.getMultiReddit(multipath)
                        .await().data.subreddits.map { it.data!! }.toMutableList()
                }
                val allNames = currentList.map { it.displayName }.toMutableSet()
                allNames.addAll(names)
                val subsData = allNames.map { SubredditData(it, null) }
                val model = MultiRedditUpdate(subreddits = subsData)
                val response = subredditRepository.updateMulti(multipath, model).await()
                _multi.postValue(response.data)
                _subreddits.postValue(response.data.subreddits.map { it.data!! }.toMutableList())
            } catch (e: HttpException) {
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateMultiReddit(model: MultiRedditUpdate) {
        coroutineScope.launch {
            try {
                _isLoading.postValue(true)
                val response = subredditRepository.updateMulti(multipath, model).await()
                _multi.postValue(response.data)
                subredditRepository.insertMultiReddit(response.data)
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}