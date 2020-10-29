package dev.gtcl.astro.ui.fragments.report

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.AstroViewModel
import dev.gtcl.astro.R
import dev.gtcl.astro.getErrorMessage
import dev.gtcl.astro.models.reddit.RuleData
import dev.gtcl.astro.models.reddit.RuleFor
import dev.gtcl.astro.models.reddit.RuleType
import dev.gtcl.astro.models.reddit.listing.Comment
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.Post
import kotlinx.coroutines.launch

class ReportVM(private val application: AstroApplication) : AstroViewModel(application) {

    private val _rules = MutableLiveData<List<RuleData>?>().apply { value = null }
    val rules: LiveData<List<RuleData>?>
        get() = _rules

    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val loading: LiveData<Boolean>
        get() = _loading

    private val _otherSelected = MutableLiveData<Boolean>().apply { value = false }
    val otherSelected: LiveData<Boolean>
        get() = _otherSelected

    fun otherRuleSelected() {
        _otherSelected.value = true
    }

    fun otherRuleUnselected() {
        _otherSelected.value = false
    }

    fun fetchRules(item: Item) {
        coroutineScope.launch {
            try {
                _loading.postValue(true)
                val rulesResponse = when (item) {
                    is Post -> subredditRepository.getRules((item).subreddit).await()
                    is Comment -> subredditRepository.getRules(item.subreddit).await()
                    else -> throw IllegalArgumentException("Invalid item to fetch rules: $item")
                }
                val rulesList = mutableListOf<RuleData>()
                when (item) {
                    is Post -> rulesList.addAll(rulesResponse.rules.filter { it.kind == RuleFor.POST || it.kind == RuleFor.ALL }
                        .map { RuleData(it.shortName, RuleType.RULE) })
                    is Comment -> rulesList.addAll(rulesResponse.rules.filter { it.kind == RuleFor.COMMENT || it.kind == RuleFor.ALL }
                        .map { RuleData(it.shortName, RuleType.RULE) })
                }
                rulesList.addAll(rulesResponse.siteRules.map { RuleData(it, RuleType.SITE_RULE) })
                rulesList.add(RuleData(application.getString(R.string.other), RuleType.OTHER))
                _rules.postValue(rulesList)
            } catch (e: Exception) {
                _errorMessage.postValue(e.getErrorMessage(application))
            } finally {
                _loading.postValue(false)
            }
        }
    }
}