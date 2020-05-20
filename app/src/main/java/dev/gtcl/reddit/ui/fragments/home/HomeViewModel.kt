package dev.gtcl.reddit.ui.fragments.home

import androidx.lifecycle.*
import dev.gtcl.reddit.*
import dev.gtcl.reddit.repositories.ListingRepository
import kotlinx.coroutines.*
import java.util.concurrent.Executors

class HomeViewModel(val application: RedditApplication): AndroidViewModel(application){

    // Repositories
    private val listingRepository = ListingRepository.getInstance(application)

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

//    // Mine
//    private val _defaultSubreddits = MutableLiveData<List<Subreddit>>()
//    val defaultSubreddits: LiveData<List<Subreddit>> = Transformations.map(_defaultSubreddits) {
//        it.sortedBy { sub -> sub.displayName.toUpperCase(Locale.US) }
//    }
//
//    fun fetchDefaultSubreddits(){
//        coroutineScope.launch {
//            try {
//                if(application.accessToken == null)
//                    _defaultSubreddits.value = listingRepository.getSubreddits("default").await().data.children.map { it.data as Subreddit }
//                else {
//                    val allSubs = mutableListOf<Subreddit>()
//                    var subs = listingRepository.getSubreddits("subscriber").await().data.children.map { it.data as Subreddit }
//                    while(subs.isNotEmpty()) {
//                        allSubs.addAll(subs)
//                        val lastSub = subs.last()
//                        subs = listingRepository.getSubreddits("subscriber", after = lastSub.name).await().data.children.map { it.data as Subreddit }
//                    }
//                    _defaultSubreddits.value = allSubs
//                }
//            } catch(e: Exception) {
//                Log.d(TAG, "Exception: $e") // TODO: Handle
//            }
//        }
//    }

    companion object {
        private val TAG = HomeViewModel::class.qualifiedName
    }
}