package dev.gtcl.reddit.repositories

import androidx.annotation.MainThread
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.network.RedditApi
import dev.gtcl.reddit.database.redditDatabase
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.models.reddit.AccountChild
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
            val databaseUser = account.asDbModel()
            database.accountDao.insert(databaseUser)
        }
    }

    @MainThread
    suspend fun deleteUserInDatabase(username: String){
        withContext(Dispatchers.IO) {
            database.accountDao.deleteUser(username)
        }
    }

    @MainThread
    fun getUsersFromDatabase() = database.accountDao.getUsers()

    companion object{
        private lateinit var INSTANCE: UserRepository
        fun getInstance(application: RedditApplication): UserRepository {
            if(!Companion::INSTANCE.isInitialized)
                INSTANCE =
                    UserRepository(application)
            return INSTANCE
        }
    }

}
