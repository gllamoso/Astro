package dev.gtcl.reddit.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.users.AccessToken
import dev.gtcl.reddit.users.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivityViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val userRepository = application.userRepository

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val _currentUser = MutableLiveData<User>()
    val currentUser: LiveData<User>
        get() = _currentUser

    fun setCurrentUser(user: User?, saveToPreferences: Boolean){
        _currentUser.value = user

        if(saveToPreferences){
            val sharedPrefs = application.getSharedPreferences(application.getString(R.string.preferences_file_key), Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                val json = Gson().toJson(user)
                putString(application.getString(R.string.current_user_key), json)
                commit()
            }
        }

        if(user == null) application.accessToken = null
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

    // --- Flow for adding a new user --- //

    fun fetchUserFromUri(uri: Uri){
        if (uri.getQueryParameter("error") != null) {
            val error = uri.getQueryParameter("error")
            Log.d("TAE", "An error has occurred : $error") // TODO: Handle error
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
                application.accessToken = userRepository.getNewAccessToken(authorization = "Basic ${getEncodedAuthString(application.baseContext)}", refreshToken = it.refreshToken!!).await()
            }
        }
    }

    val fetchAccessTokenIfNecessary: suspend () -> Unit = {
        currentUser.value?.let {
            if(application.accessToken == null || application.accessToken!!.isExpired())
                application.accessToken = userRepository.getNewAccessToken(authorization = "Basic ${getEncodedAuthString(application.baseContext)}", refreshToken = it.refreshToken!!).await()
        }
    }

    private fun fetchAccessToken(code: String){
        coroutineScope.launch {
            val result = userRepository.postCode(
                authorization = "Basic ${getEncodedAuthString(application.baseContext)}",
                code = code,
                redirectUri = application.getString(R.string.redirect_uri)).await()
            application.accessToken = result
            val user = userRepository.getUserInfo(result.value).await()
            result.refreshToken?.let{
                user.refreshToken = it
                userRepository.insertUserToDatabase(user)
                setCurrentUser(user, true)
            }
        }
    }
}
