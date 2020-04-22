package dev.gtcl.reddit.ui.fragments.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountFragmentViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _account = MutableLiveData<Account>()
    val account: LiveData<Account>
        get() = _account

    var username: String? = null

    fun fetchAccount(user: String?){
        username = user
        coroutineScope.launch {
            if(user != null)
                _account.value = userRepository.getAccountInfo(user).await().data
            else
                _account.value = userRepository.getCurrentAccountInfo().await()
        }
    }

}