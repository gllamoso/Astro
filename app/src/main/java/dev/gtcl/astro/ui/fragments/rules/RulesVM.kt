package dev.gtcl.astro.ui.fragments.rules

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.Rule
import kotlinx.coroutines.launch

class RulesVM(private val application: AstroApplication) : AstroViewModel(application) {

    private val _rules = MutableLiveData<List<Rule>?>().apply { value = null }
    val rules: LiveData<List<Rule>?>
        get() = _rules

    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val loading: LiveData<Boolean>
        get() = _loading

    fun fetchRules(displayName: String) {
        coroutineScope.launch {
            try {
                _loading.postValue(true)
                _rules.postValue(subredditRepository.getRules(displayName).await().rules)
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _loading.postValue(false)
            }
        }
    }
}