package dev.gtcl.reddit.ui.fragments.subreddits.mine

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.database.DbSubreddit
import dev.gtcl.reddit.database.asDomainModel
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.repositories.SubredditRepository
import kotlinx.coroutines.*

class MineViewModel(private val application: RedditApplication): AndroidViewModel(application){
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val multiReddits = subredditRepository.getMyMultiRedditsDb()
    var multiRedditsInitialized = false
    val subscribedSubs = Transformations.map(subredditRepository.getSubscribedSubsLive()){ it.asDomainModel() }!!
    var subscribedInitialized = false

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    companion object{
        val TAG = ::MineViewModel.javaClass.simpleName
    }
}