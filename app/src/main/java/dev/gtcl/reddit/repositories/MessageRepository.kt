package dev.gtcl.reddit.repositories

import dev.gtcl.reddit.MessageWhere
import dev.gtcl.reddit.NotLoggedInException
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.listing.ListingResponse
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.Deferred
import retrofit2.Response

class MessageRepository private constructor(private val application: RedditApplication){

    fun getMessages(where: MessageWhere, after: String? = null, limit: Int? = null): Deferred<ListingResponse>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.getMessages(application.accessToken!!.authorizationHeader, where, after, limit)
    }

    fun sendMessage(to: String, subject: String, markdown: String): Deferred<Response<Unit>>{
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.sendMessage(application.accessToken!!.authorizationHeader, to, subject, markdown)
    }

    companion object{
        private lateinit var INSTANCE: MessageRepository
        fun getInstance(application: RedditApplication): MessageRepository {
            if(!Companion::INSTANCE.isInitialized){
                INSTANCE = MessageRepository(application)
            }
            return INSTANCE
        }
    }
}