package dev.gtcl.reddit.ui.activities

import androidx.lifecycle.ViewModel
import dev.gtcl.reddit.*
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class MainActivityVM(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val allUsers = userRepository.getAllUsers()

}
