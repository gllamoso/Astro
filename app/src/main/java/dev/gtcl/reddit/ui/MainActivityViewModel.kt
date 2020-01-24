package dev.gtcl.reddit.ui

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.posts.RedditPost
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.users.User
import dev.gtcl.reddit.Listing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivityViewModel(val application: RedditApplication): ViewModel() {

    // Repos
    private val postRepository = application.postRepository
    private val userRepository = application.userRepository

    // Scopes
    private var viewModelJob = Job()
    private val coroutineScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    val allUsers = userRepository.getUsersFromDatabase()

    private val _currentUser = MutableLiveData<User>()
    val currentUser: LiveData<User>
        get() = _currentUser

    var accessToken: String? = null

    private val _subredditSelected = MutableLiveData<Subreddit>()
    val subredditSelected: LiveData<Subreddit>
        get() = _subredditSelected

    private val _sortSelected = MutableLiveData<PostSort>()
    val sortSelected: LiveData<PostSort>
        get() = _sortSelected

    private val _timeSelected = MutableLiveData<Time>()
    val timeSelected: LiveData<Time>
        get() = _timeSelected

    private val postListingsOfSubreddit = MutableLiveData<Listing<RedditPost>>()
    val posts = Transformations.switchMap(postListingsOfSubreddit) { it.pagedList }
    val networkState = Transformations.switchMap(postListingsOfSubreddit) { it.networkState }
    val refreshState = Transformations.switchMap(postListingsOfSubreddit) { it.refreshState }

    fun refresh() = postListingsOfSubreddit.value?.refresh?.invoke()

    fun retry() {
        val listing = postListingsOfSubreddit.value
        listing?.retry?.invoke()
    }

    fun getPosts(subreddit: Subreddit?, sortBy: PostSort = PostSort.HOT, timePeriod: Time? = null): Boolean{
        if(subredditSelected.value?.displayName == subreddit?.displayName && sortSelected.value == sortBy && timeSelected.value == timePeriod)
            return false

        _subredditSelected.value = subreddit
        _sortSelected.value = sortBy
        _timeSelected.value = timePeriod
        postListingsOfSubreddit.value = postRepository.getPostsOfSubreddit(subreddit!!, sortBy, timePeriod, 30)
        return true
    }

    fun getCodeFromUri(uri: Uri){
        if (uri.getQueryParameter("error") != null) {
            val error = uri.getQueryParameter("error")
            Log.d("TAE", "An error has occurred : $error") // TODO: Handle error
        } else {
            val state = uri.getQueryParameter("state")
            if(state == STATE) {
                val code = uri.getQueryParameter("code")
                code?.let { getAccessToken(it) }
            }
        }
    }

    private fun getAccessToken(code: String){
        coroutineScope.launch {
            val result = userRepository.postCode(
                authorization = "Basic ${getEncodedAuthString()}",
                code = code,
                redirectUri = application.getString(R.string.redirect_uri)).await()
            accessToken = result.accessToken
            val user = userRepository.getUserInfo(result.accessToken).await()
            result.refreshToken?.let{
                user.refreshToken = it
                userRepository.insertUserToDatabase(user)
                setCurrentUser(user)
            }
        }
    }

    fun deleteUserFromDatabase(username: String){
        coroutineScope.launch {
            userRepository.deleteUserInDatabase(username)
            currentUser.value?.let {
                if(it.name == username)
                    setCurrentUser(null)
            }
        }
    }

    fun setCurrentUser(user: User?){
        if(_currentUser.value == user) return

        if(user == null) accessToken = null

        _currentUser.value = user
        val sharedPrefs = application.getSharedPreferences(application.getString(R.string.preferences_file_key), Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            val json = Gson().toJson(user)
            putString(application.getString(R.string.current_user_key), json)
            commit()
        }

    }



    fun renewAccessToken(){
        coroutineScope.launch {
            currentUser.value?.let {
                accessToken = userRepository.getNewAccessToken(authorization = "Basic ${getEncodedAuthString()}", refreshToken = it.refreshToken!!).await().accessToken
                Log.d("TAE", "new access token: $accessToken")
            }

        }
    }

    private fun getEncodedAuthString(): String{
        val clientID = application.getText(R.string.client_id)
        val authString = "$clientID:"
        return Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)
    }
}
