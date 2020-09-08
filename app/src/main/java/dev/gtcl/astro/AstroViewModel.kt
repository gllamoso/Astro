package dev.gtcl.astro

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.repositories.GfycatRepository
import dev.gtcl.astro.repositories.ImgurRepository
import dev.gtcl.astro.repositories.reddit.ListingRepository
import dev.gtcl.astro.repositories.reddit.MiscRepository
import dev.gtcl.astro.repositories.reddit.SubredditRepository
import dev.gtcl.astro.repositories.reddit.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class AstroViewModel(application: AstroApplication) : AndroidViewModel(application) {
    // Repos
    protected val listingRepository = ListingRepository.getInstance(application)
    protected val miscRepository = MiscRepository.getInstance(application)
    protected val subredditRepository = SubredditRepository.getInstance(application)
    protected val userRepository = UserRepository.getInstance(application)
    protected val gfycatRepository = GfycatRepository.getInstance()
    protected val imgurRepository = ImgurRepository.getInstance()

    // Scopes
    private val viewModelJob = Job()
    protected val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Default)

    protected val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun errorMessageObserved() {
        _errorMessage.value = null
    }
}