package dev.gtcl.reddit.listings.users

import androidx.annotation.MainThread
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.network.RedditApi
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.listings.Account
import dev.gtcl.reddit.listings.AccountChild
import dev.gtcl.reddit.listings.asDatabaseModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository private constructor(val application: RedditApplication) { // TODO: Delete
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
    fun getCurrentAccountInfo(): Deferred<Account> =
        RedditApi.oauth.getCurrentAccountInfo("bearer ${application.accessToken!!.value}")

    @MainThread
    fun getAccountInfo(username: String): Deferred<AccountChild>{
        return if(application.accessToken != null)
            RedditApi.oauth.getUserInfo("bearer ${application.accessToken!!.value}", username)
        else
            RedditApi.base.getUserInfo(null, username)
    }

    // --- DATABASE

    @MainThread
    suspend fun insertUserToDatabase(account: Account){
        withContext(Dispatchers.IO){
            val databaseUser = account.asDatabaseModel()
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
