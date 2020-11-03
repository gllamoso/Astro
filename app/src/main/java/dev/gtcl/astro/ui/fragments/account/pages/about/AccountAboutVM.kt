package dev.gtcl.astro.ui.fragments.account.pages.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonDataException
import dev.gtcl.astro.*
import dev.gtcl.astro.models.reddit.listing.*
import kotlinx.coroutines.launch

class AccountAboutVM(val application: AstroApplication) : AstroViewModel(application) {

    private val _account = MutableLiveData<Account?>().apply { value = null }
    val account: LiveData<Account?>
        get() = _account

    private val _trophies = MutableLiveData<List<Trophy>>().apply { value = listOf() }
    val trophies: LiveData<List<Trophy>>
        get() = _trophies

    private val _multiReddits = MutableLiveData<List<MultiReddit>>().apply { value = listOf() }
    val multiReddits: LiveData<List<MultiReddit>?>
        get() = _multiReddits

    private val _moderatedSubs =
        MutableLiveData<List<SubredditInModeratedList>>().apply { value = listOf() }
    val moderatedSubs: LiveData<List<SubredditInModeratedList>>
        get() = _moderatedSubs

    fun fetchAccount(user: String?) {
        coroutineScope.launch {
            try {
                val account = if (user != null) {
                    userRepository.getAccountInfo(user).await().data
                } else {
                    userRepository.getCurrentAccountInfo().await()
                }
                account.subreddit?.parseDescription()
                _account.postValue(account)
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

    fun fetchTrophies(user: String?) {
        coroutineScope.launch {
            try {
                val trophies = miscRepository.getTrophies(
                    user ?: (application.currentAccount ?: return@launch).name
                ).await().data.trophies.map { (data) -> data }
                _trophies.postValue(trophies)
            } catch (e: Exception) {
                _trophies.postValue(listOf())
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
                    user ?: (application.currentAccount ?: return@launch).name
                ).await().map { it.data }.filter { it.visibility == Visibility.PUBLIC }
                publicFeeds.parseAllText()
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