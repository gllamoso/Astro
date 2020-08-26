package dev.gtcl.reddit.ui.fragments.report

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.RuleFor
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.repositories.reddit.SubredditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReportVM(private val application: RedditApplication): AndroidViewModel(application) {

    // Repos
    private val subredditRepository = SubredditRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?>
        get() = _errorMessage

    private val _rules = MutableLiveData<List<RuleData>?>().apply { value = null }
    val rules: LiveData<List<RuleData>?>
        get() = _rules

    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val loading: LiveData<Boolean>
        get() = _loading

    private val _otherSelected = MutableLiveData<Boolean>().apply { value = false }
    val otherSelected: LiveData<Boolean>
        get() = _otherSelected

    fun errorMessageObserved(){
        _errorMessage.value = null
    }

    fun otherRuleSelected(){
        _otherSelected.value = true
    }

    fun otherRuleUnselected(){
        _otherSelected.value = false
    }

    fun fetchRules(item: Item){
        coroutineScope.launch {
            try{
                _loading.value = true
                val rulesResponse = when(item){
                    is Post -> subredditRepository.getRules((item).subreddit).await()
                    is Comment -> subredditRepository.getRules(item.subreddit).await()
                    else -> throw IllegalArgumentException("Invalid item to fetch rules: $item")
                }
                val rulesList = mutableListOf<RuleData>()
                when(item){
                    is Post -> rulesList.addAll(rulesResponse.rules.filter { it.kind == RuleFor.POST || it.kind == RuleFor.ALL }.map { RuleData(it.shortName, RuleType.RULE) })
                    is Comment -> rulesList.addAll(rulesResponse.rules.filter { it.kind == RuleFor.COMMENT || it.kind == RuleFor.ALL }.map { RuleData(it.shortName, RuleType.RULE) })
                }
                rulesList.addAll(rulesResponse.siteRules.map { RuleData(it, RuleType.SITE_RULE) })
                rulesList.add(RuleData(application.getString(R.string.other), RuleType.OTHER))
                _rules.value = rulesList
            } catch (e: Exception){
                _errorMessage.value = e.getErrorMessage(application)
            } finally {
                _loading.value = false
            }
        }
    }
}