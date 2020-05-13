package dev.gtcl.reddit.ui.activities.main

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.ItemsRead
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.repositories.ListingRepository
import dev.gtcl.reddit.repositories.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivityViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = UserRepository.getInstance(application)
    private val postRepository = ListingRepository.getInstance(application)

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _currentUser = MutableLiveData<Account>()
    val currentAccount: LiveData<Account>
        get() = _currentUser

    val allUsers = userRepository.getUsersFromDatabase()

    private val _ready = MutableLiveData<Boolean>()
    val ready: LiveData<Boolean>
        get() = _ready

    fun readyComplete(){
        _ready.value = null
    }

    fun setCurrentUser(account: Account?, saveToPreferences: Boolean){
        _currentUser.value = account
        application.currentAccount = account

        if(saveToPreferences){
            val sharedPrefs = application.getSharedPreferences(application.getString(R.string.preferences_file_key), Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                val json = Gson().toJson(account)
                putString(CURRENT_USER_KEY, json)
                commit()
            }
        }

        if(account == null) {
            application.accessToken = null
            _ready.value = true
        }
        else fetchAccessToken()
    }

    fun deleteUserFromDatabase(username: String){
        coroutineScope.launch {
            userRepository.deleteUserInDatabase(username)
            currentAccount.value?.let {
                if(it.name == username)
                    setCurrentUser(null, true)
            }
        }
    }

    private val _startSignInActivity = MutableLiveData<Boolean>()
    val startSignInActivity: LiveData<Boolean>
        get() = _startSignInActivity

    fun startSignInActivity(){
        _startSignInActivity.value = true
    }

    fun startSignInActivityFinished(){
        _startSignInActivity.value = null
    }

    // --- Flow for adding a new user --- //

    fun fetchUserFromUri(uri: Uri){
        if (uri.getQueryParameter("error") != null) {
            val error = uri.getQueryParameter("error")
            Log.d("TAE", "An error has occurred : $error") // TODO: Handle error: Have String live data error
        } else {
            val state = uri.getQueryParameter("state")
            if(state == STATE) {
                val code = uri.getQueryParameter("code")
                code?.let { fetchAccessToken(it) }
            }
        }
    }


    // TODO: create loading animation?
    private fun fetchAccessToken(){
        coroutineScope.launch {
            currentAccount.value?.let {
                val accessToken = userRepository.getNewAccessToken(authorization = "Basic ${getEncodedAuthString(application.baseContext)}", refreshToken = it.refreshToken!!).await()
                application.accessToken = accessToken
                _ready.value = true
            }
        }
    }

    private fun fetchAccessToken(code: String){
        coroutineScope.launch {
            val result = userRepository.postCode(
                authorization = "Basic ${getEncodedAuthString(application.baseContext)}",
                code = code,
                redirectUri = application.getString(R.string.redirect_uri)).await()
            application.accessToken = result
            val user = userRepository.getCurrentAccountInfo().await()
            result.refreshToken?.let{
                user.refreshToken = it
                userRepository.insertUserToDatabase(user)
                setCurrentUser(user, true)
            }
        }
    }

    // Read posts
    val allReadPosts = postRepository.getReadPostsFromDatabase()

    fun addReadPost(itemsRead: ItemsRead) {
        coroutineScope.launch {
            postRepository.insertReadPostToDatabase(itemsRead)
        }
    }
}
