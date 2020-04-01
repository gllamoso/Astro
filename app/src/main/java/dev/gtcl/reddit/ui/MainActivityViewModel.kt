package dev.gtcl.reddit.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.ReadListing
import dev.gtcl.reddit.listings.users.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivityViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = application.userRepository
    private val postRepository = application.listingRepository

    // Scopes
    private val viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _currentUser = MutableLiveData<User>()
    val currentUser: LiveData<User>
        get() = _currentUser

    val allUsers = userRepository.getUsersFromDatabase()

    private val _fetchData = MutableLiveData<Boolean>()
    val fetchData: LiveData<Boolean>
        get() = _fetchData

    fun setCurrentUser(user: User?, saveToPreferences: Boolean){
        _currentUser.value = user
        application.currentUser = user

        if(saveToPreferences){
            val sharedPrefs = application.getSharedPreferences(application.getString(R.string.preferences_file_key), Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                val json = Gson().toJson(user)
                putString(application.getString(R.string.current_user_key), json)
                commit()
            }
        }

        if(user == null) {
            application.accessToken = null
            _fetchData.value = true
        }
        else fetchAccessToken()
    }

    fun deleteUserFromDatabase(username: String){
        coroutineScope.launch {
            userRepository.deleteUserInDatabase(username)
            currentUser.value?.let {
                if(it.name == username)
                    setCurrentUser(null, true)
            }
        }
    }

    fun dataFetchComplete(){
        _fetchData.value = null
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
            currentUser.value?.let {
                val accessToken = userRepository.getNewAccessToken(authorization = "Basic ${getEncodedAuthString(application.baseContext)}", refreshToken = it.refreshToken!!).await()
                application.accessToken = accessToken
                _fetchData.value = true
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
            val user = userRepository.getUserInfo().await()
            result.refreshToken?.let{
                user.refreshToken = it
                userRepository.insertUserToDatabase(user)
                setCurrentUser(user, true)
            }
        }
    }

    private val _openDrawer = MutableLiveData<Boolean>()
    val openDrawer: LiveData<Boolean>
        get() = _openDrawer
    fun openDrawer(){
        _openDrawer.value = true
    }

    fun openDrawerComplete(){
        _openDrawer.value = null
    }

    private val _allowDrawerSwipe = MutableLiveData<Boolean>()
    val allowDrawerSwipe: LiveData<Boolean>
        get() = _allowDrawerSwipe

    fun allowDrawerSwipe(allow: Boolean){
        _allowDrawerSwipe.value = allow
    }

    // Read posts
    val allReadPosts = postRepository.getReadPostsFromDatabase()

    fun addReadPost(readListing: ReadListing) {
        coroutineScope.launch {
            postRepository.insertReadPostToDatabase(readListing)
        }
    }
}
