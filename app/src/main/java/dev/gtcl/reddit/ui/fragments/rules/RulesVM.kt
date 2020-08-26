package dev.gtcl.reddit.ui.fragments.rules

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.getErrorMessage
import dev.gtcl.reddit.models.reddit.Rule
import dev.gtcl.reddit.repositories.reddit.SubredditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RulesVM(private val application: RedditApplication): AndroidViewModel(application){

    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _rules = MutableLiveData<List<Rule>?>().apply { value = null }
    val rules: LiveData<List<Rule>?>
        get() = _rules

    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val loading: LiveData<Boolean>
        get() = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    fun errorMessageObserved(){
        _errorMessage.value = null
    }

    fun fetchRules(displayName: String){
        coroutineScope.launch {
            try{
                _loading.value = true
                _rules.value = subredditRepository.getRules(displayName).await().rules
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            } finally {
                _loading.value = false
            }
        }
    }
}