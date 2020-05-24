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

    companion object {
        private val TAG = HomeViewModel::class.qualifiedName
    }
}