package dev.gtcl.astro.repositories.reddit

import androidx.annotation.MainThread
import dev.gtcl.astro.NotLoggedInException
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.network.RedditApi
import dev.gtcl.astro.database.redditDatabase
import dev.gtcl.astro.models.reddit.AccessToken
import dev.gtcl.astro.models.reddit.FriendRequest
import dev.gtcl.astro.models.reddit.User
import dev.gtcl.astro.models.reddit.UserList
import dev.gtcl.astro.models.reddit.listing.Account
import dev.gtcl.astro.models.reddit.listing.AccountChild
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class UserRepository private constructor(val application: AstroApplication) {
    private val database = redditDatabase(application)

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
        RedditApi.oauth.getCurrentAccountInfo(application.accessToken!!.authorizationHeader)

    @MainThread
    fun getAccount(accessToken: AccessToken): Deferred<Account> =
        RedditApi.oauth.getCurrentAccountInfo(accessToken.authorizationHeader)

    @MainThread
    fun getAccountInfo(username: String): Deferred<AccountChild>{
        return if(application.accessToken != null){
            RedditApi.oauth.getUserInfo(application.accessToken!!.authorizationHeader, username)
        }
        else{
            RedditApi.base.getUserInfo(null, username)
        }
    }

    @MainThread
    fun getFriends(): Deferred<List<UserList>>{
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.getFriends(application.accessToken!!.authorizationHeader)
    }

    @MainThread
    fun addFriend(username: String): Deferred<User>{
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.addFriend(application.accessToken!!.authorizationHeader, username, FriendRequest(username))
    }

    @MainThread
    fun removeFriend(username: String): Deferred<Response<Unit>>{
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.removeFriend(application.accessToken!!.authorizationHeader, username, FriendRequest(username))
    }

    @MainThread
    fun getBlocked(): Deferred<UserList>{
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.getBlocked(application.accessToken!!.authorizationHeader)
    }

    @MainThread
    fun blockUser(username: String): Deferred<Response<Unit>>{
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.blockUser(application.accessToken!!.authorizationHeader, username)
    }

    @MainThread
    fun unblockUser(username: String): Deferred<Response<Unit>>{
        if(application.accessToken == null || application.currentAccount == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.unblockUser(application.accessToken!!.authorizationHeader, application.currentAccount!!.fullId, username)
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
    fun getAllUsers() = database.accountDao.getUsers()

    companion object{
        private lateinit var INSTANCE: UserRepository
        fun getInstance(application: AstroApplication): UserRepository {
            if(!Companion::INSTANCE.isInitialized){
                INSTANCE = UserRepository(application)
            }
            return INSTANCE
        }
    }

}
