package dev.gtcl.astro.ui.fragments.account.pages.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonDataException
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.listing.*
import kotlinx.coroutines.launch

class AccountAboutVM(val application: AstroApplication) : AstroViewModel(application) {

    private val _account = MutableLiveData<Account>()
    val account: LiveData<Account>
        get() = _account

    private val _awards = MutableLiveData<List<Award>>().apply { value = listOf() }
    val awards: LiveData<List<Award>>
        get() = _awards

    private val _multiReddits = MutableLiveData<List<MultiReddit>>().apply { value = listOf() }
    val multiReddits: LiveData<List<MultiReddit>?>
        get() = _multiReddits

    private val _moderatedSubs = MutableLiveData<List<SubredditInModeratedList>>().apply { value = listOf() }
    val moderatedSubs: LiveData<List<SubredditInModeratedList>>
        get() = _moderatedSubs

    fun fetchAccount(user: String?) {
        coroutineScope.launch {
            try {
                if (user != null) {
                    _account.postValue(userRepository.getAccountInfo(user).await().data)
                } else {
                    _account.postValue(userRepository.getCurrentAccountInfo().await())
                }
            } catch (e: Exception) {
                _errorMessage.postValue(
                    if (e is JsonDataException) {
                        application.getString(R.string.account_error)
                    } else {
                        e.getErrorMessage(application)
                    }
                )
            }
        }
    }

    fun fetchAwards(user: String?) {
        coroutineScope.launch {
            try {
                val trophies = miscRepository.getAwards(
                    user ?: application.currentAccount!!.name
                ).await().data.trophies.map { (data) -> data }
                _awards.postValue(trophies)
            } catch (e: Exception) {
                _awards.postValue(listOf())
                _errorMessage.postValue(
                    if (e is JsonDataException) {
                        application.getString(R.string.account_error)
                    } else {
                        e.getErrorMessage(application)
                    }
                )
            }
        }
    }

    fun fetchPublicFeeds(user: String?) {
        coroutineScope.launch {
            try {
                val publicFeeds = subredditRepository.getMultiReddits(
                    user ?: application.currentAccount!!.name
                ).await().map { it.data }.filter { it.visibility == Visibility.PUBLIC }
                _multiReddits.postValue(publicFeeds)
            } catch (e: Exception) {
                _multiReddits.postValue(listOf())
                _errorMessage.postValue(
                    if (e is JsonDataException) {
                        application.getString(R.string.account_error)
                    } else {
                        e.getErrorMessage(application)
                    }
                )
            }
        }
    }

    fun fetchModeratedSubs(user: String?) {
        coroutineScope.launch {
            try {
                val moderatedSubs = subredditRepository.getModeratedSubs(
                    user ?: (application.currentAccount ?: return@launch).name
                ).await().data
                _moderatedSubs.postValue(moderatedSubs ?: listOf())
            } catch (e: Exception) {
                _errorMessage.postValue(
                    if (e is JsonDataException) {
                        application.getString(R.string.account_error)
                    } else {
                        e.getErrorMessage(application)
                    }
                )
            }
        }
    }
}