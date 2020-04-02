package dev.gtcl.reddit.listings.users

import androidx.annotation.MainThread
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.network.RedditApi
import dev.gtcl.reddit.database.redditDatabase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository private constructor(val application: RedditApplication) {
    private val database = redditDatabase(application)
    val redirectUri = application.getString(R.string.redirect_uri)

    // --- NETWORK

    @MainThread
    fun postCode(authorization: String, code: String, redirectUri: String) =
        RedditApi.base.postCode(authorization = authorization, code = code, redirectUri = redirectUri)

    @MainThread
    fun getNewAccessToken(authorization: String, refreshToken: String) =
        RedditApi.base.getAccessToken(
            authorization = authorization,
            refreshToken = refreshToken
        )

    @MainThread
    fun getUserInfo(): Deferred<User> =
        RedditApi.oauth.getCurrentUserInfo("bearer ${application.accessToken!!.value}")

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

    companion object{
        private lateinit var INSTANCE: UserRepository
        fun getInstance(application: RedditApplication): UserRepository{
            if(!::INSTANCE.isInitialized)
                INSTANCE = UserRepository(application)
            return INSTANCE
        }
    }

}
