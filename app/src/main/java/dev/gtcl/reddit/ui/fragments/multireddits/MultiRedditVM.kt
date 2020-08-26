package dev.gtcl.reddit.ui.fragments.multireddits

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.models.reddit.listing.MultiReddit
import dev.gtcl.reddit.models.reddit.listing.MultiRedditUpdate
import dev.gtcl.reddit.models.reddit.listing.Subreddit
import dev.gtcl.reddit.models.reddit.listing.SubredditData
import dev.gtcl.reddit.repositories.reddit.SubredditRepository
import kotlinx.coroutines.*
import retrofit2.HttpException

class MultiRedditVM(application: RedditApplication): AndroidViewModel(application){

    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _subreddits = MutableLiveData<MutableList<Subreddit>>(). apply{ value = mutableListOf() }
    val subreddits: LiveData<MutableList<Subreddit>>
        get() = _subreddits

    private val _multi = MutableLiveData<MultiReddit>()
    val multi: LiveData<MultiReddit>
        get() = _multi

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private lateinit var multipath: String

    private var _initialized: Boolean = false
    val initialized: Boolean
        get() = _initialized

    fun fetchMultiReddit(subscription: Subscription){
        coroutineScope.launch {
            multipath = subscription.url.replaceFirst("/", "")
            val response = subredditRepository.getMultiReddit(multipath).await()
            _multi.value = response.data
            _subreddits.value = response.data.subreddits.map { it.data!! }.toMutableList()
            _initialized = true
        }
    }

    fun remove(subreddit: Subreddit, position: Int){
        coroutineScope.launch {
            try {
                _subreddits.value?.removeAt(position)
                _subreddits.value = _subreddits.value ?: mutableListOf()
                val response = subredditRepository.deleteSubredditFromMultiReddit(multipath, subreddit).await()
                if(response.code() != 200){
                    throw HttpException(response)
                }
            } catch (e: Exception){
                _errorMessage.value = e.toString()
            }
        }
    }

    fun addSubredditsToMultiReddit(names: List<String>){
        coroutineScope.launch {
            try {
                val currentList: MutableList<Subreddit> = if(initialized){
                    _subreddits.value ?: mutableListOf()
                } else {
                    subredditRepository.getMultiReddit(multipath).await().data.subreddits.map{ it.data!! }.toMutableList()
                }
                val allNames = currentList.map { it.displayName }.toMutableSet()
                allNames.addAll(names)
                val subsData = allNames.map { SubredditData(it, null) }
                val model = MultiRedditUpdate(subreddits = subsData)
                val response = subredditRepository.updateMulti(multipath, model).await()
                _multi.value = response.data
                _subreddits.value = response.data.subreddits.map{ it.data!! }.toMutableList()
            } catch (e: HttpException){
                _errorMessage.value = e.toString()
            }
        }
    }

    fun updateMultiReddit(model: MultiRedditUpdate){
        coroutineScope.launch {
            try {
                val response = subredditRepository.updateMulti(multipath, model).await()
                _multi.value = response.data
                subredditRepository.insertMultiReddit(response.data)
            } catch (e: HttpException){
                _errorMessage.value = e.toString()
            }
        }
    }
}