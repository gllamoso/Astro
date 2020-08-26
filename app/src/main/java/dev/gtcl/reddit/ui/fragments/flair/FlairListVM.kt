package dev.gtcl.reddit.ui.fragments.flair

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.getErrorMessage
import dev.gtcl.reddit.models.reddit.listing.Flair
import dev.gtcl.reddit.repositories.reddit.SubredditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException

class FlairListVM(private val application: RedditApplication): AndroidViewModel(application) {

    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _flairs = MutableLiveData<List<Flair>?>().apply { value = null }
    val flairs: LiveData<List<Flair>?>
        get() = _flairs

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val loading: LiveData<Boolean>
        get() = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun fetchFlairs(srName: String){
        coroutineScope.launch {
            try{
                _loading.value = true
                _title.value = srName
                _flairs.value = subredditRepository.getFlairs(srName).await()
            } catch (e: Exception){
                if(e is HttpException && e.code() == 403){
                    _flairs.value = listOf()
                } else {
                    _errorMessage.value = e.getErrorMessage(application)
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun errorMessageObserved(){
        _errorMessage.value = null
    }
}