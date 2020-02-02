package dev.gtcl.reddit.users

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.network.RedditApi
import dev.gtcl.reddit.database.redditDatabase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository internal constructor(val application: RedditApplication) {
    private val database = redditDatabase(application)
    val redirectUri = application.getString(R.string.redirect_uri)

    // --- NETWORK

    @MainThread
    fun postCode(authorization: String, code: String, redirectUri: String) =
        RedditApi.retrofitServiceWithNoAuth.postCode(authorization = authorization, code = code, redirectUri = redirectUri)

    @MainThread
    fun getNewAccessToken(authorization: String, refreshToken: String) =
        RedditApi.retrofitServiceWithNoAuth.getAccessToken(
            authorization = authorization,
            refreshToken = refreshToken
        )

    @MainThread
    fun getUserInfo(authorization: String): Deferred<User> =
        RedditApi.retrofitServiceWithAuth.getCurrentUserInfo("bearer $authorization")

    // --- DATABASE

    @MainThread
    suspend fun insertUserToDatabase(user: User){
        withContext(Dispatchers.IO){
            val databaseUser = user.asDatabaseModel()
            database.userDao.insert(databaseUser)
        }
    }

    @MainThread
    suspend fun deleteUserInDatabase(username: String){
        withContext(Dispatchers.IO) {
            database.userDao.deleteUser(username)
        }
    }

    @MainThread
    fun getUsersFromDatabase() = database.userDao.getUsers()

}

// TODO: implement singleton
private lateinit var INSTANCE: UserRepository
fun getUserRepository(application: RedditApplication): UserRepository{
    synchronized(UserRepository::class.java){
        if(!::INSTANCE.isInitialized)
            INSTANCE = UserRepository(application)
    }
    return INSTANCE
}
