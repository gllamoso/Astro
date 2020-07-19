package dev.gtcl.reddit.repositories

import dev.gtcl.reddit.MessageWhere
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.models.reddit.listing.ListingResponse
import dev.gtcl.reddit.network.RedditApi
import kotlinx.coroutines.Deferred

class MessageRepository private constructor(private val application: RedditApplication){

    fun getMessages(where: MessageWhere, after: String? = null, limit: Int? = null): Deferred<ListingResponse>{
        if(application.accessToken == null) {
            throw IllegalStateException(application.getString(R.string.user_must_be_logged_in))
        }
        return RedditApi.oauth.getMessages(application.accessToken!!.authorizationHeader, where, after, limit)
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