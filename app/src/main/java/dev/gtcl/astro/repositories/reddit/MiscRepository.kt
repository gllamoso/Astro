package dev.gtcl.astro.repositories.reddit

import androidx.annotation.MainThread
import dev.gtcl.astro.NotLoggedInException
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.Vote
import dev.gtcl.astro.models.reddit.MoreChildrenResponse
import dev.gtcl.astro.models.reddit.listing.Flair
import dev.gtcl.astro.models.reddit.listing.Message
import dev.gtcl.astro.models.reddit.listing.TrophyListingResponse
import dev.gtcl.astro.network.RedditApi
import kotlinx.coroutines.Deferred
import retrofit2.Response

class MiscRepository private constructor(private val application: AstroApplication){

    @MainThread
    fun vote(fullname: String, vote: Vote): Deferred<Response<Unit>> {
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.vote(application.accessToken!!.authorizationHeader, fullname, vote.value)
    }

    @MainThread
    fun save(id: String): Deferred<Response<Unit>> {
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.save(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun unsave(id: String): Deferred<Response<Unit>> {
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.unsave(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun hide(id: String): Deferred<Response<Unit>> {
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.hide(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun unhide(id: String): Deferred<Response<Unit>> {
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.unhide(application.accessToken!!.authorizationHeader, id)
    }

    @MainThread
    fun report(id: String, ruleReason: String? = null, siteReason: String? = null, otherReason: String? = null): Deferred<Response<Unit>> {
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.report(application.accessToken!!.authorizationHeader, id, ruleReason, siteReason, otherReason)
    }

    @MainThread
    fun markNsfw(id: String, nsfw: Boolean): Deferred<Response<Unit>> {
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return if(nsfw) {
            RedditApi.oauth.markNsfw(application.accessToken!!.authorizationHeader, id)
        } else {
            RedditApi.oauth.unmarkNsfw(application.accessToken!!.authorizationHeader, id)
        }
    }

    @MainThread
    fun markSpoiler(id: String, spoiler: Boolean): Deferred<Response<Unit>> {
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return if(spoiler) {
            RedditApi.oauth.markSpoiler(application.accessToken!!. authorizationHeader, id)
        } else {
            RedditApi.oauth.unmarkSpoiler(application.accessToken!!.authorizationHeader, id)
        }
    }

    @MainThread
    fun sendRepliesToInbox(id: String, state: Boolean): Deferred<Response<Unit>>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.setSendReplies(
            application.accessToken!!.authorizationHeader,
            id,
            state
        )
    }

    @MainThread
    fun setFlair(id: String, flair: Flair?): Deferred<Response<Unit>> {
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.setFlair(application.accessToken!!.authorizationHeader, id, flair?.text, flair?.id)
    }

    @MainThread
    fun delete(thingId: String): Deferred<Response<Unit>> {
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.deleteThing(application.accessToken!!.authorizationHeader, thingId)
    }

    @MainThread
    fun getAwards(user: String): Deferred<TrophyListingResponse> {
        return if(application.accessToken == null) RedditApi.base.getAwards(null, user)
        else RedditApi.oauth.getAwards(application.accessToken!!.authorizationHeader, user)
    }

    @MainThread
    fun editText(thingId: String, text: String): Deferred<MoreChildrenResponse> {
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.editText(application.accessToken!!.authorizationHeader, thingId, text)
    }

    @MainThread
    fun sendMessage(to: String, subject: String, markdown: String): Deferred<Response<Unit>>{
        if(application.accessToken == null){
            throw NotLoggedInException()
        }
        return RedditApi.oauth.sendMessage(application.accessToken!!.authorizationHeader, to, subject, markdown)
    }

    @MainThread
    fun addComment(parentName: String, body: String): Deferred<MoreChildrenResponse>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.addComment(application.accessToken!!.authorizationHeader, parentName, body)
    }

    @MainThread
    fun block(message: Message): Deferred<Response<Unit>> {
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.blockMessage(application.accessToken!!.authorizationHeader, message.name)
    }

    @MainThread
    fun markMessage(message: Message, read: Boolean): Deferred<Response<Unit>>{
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return if(read){
            RedditApi.oauth.readMessage(application.accessToken!!.authorizationHeader, message.name)
        } else {
            RedditApi.oauth.unreadMessage(application.accessToken!!.authorizationHeader, message.name)
        }
    }

    @MainThread
    fun deleteMessage(message: Message): Deferred<Response<Unit>> {
        if(application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.deleteMessage(application.accessToken!!.authorizationHeader, message.name)
    }

    companion object{
        private lateinit var INSTANCE: MiscRepository
        fun getInstance(application: AstroApplication): MiscRepository{
            if(!Companion::INSTANCE.isInitialized){
                INSTANCE = MiscRepository(application)
            }
            return INSTANCE
        }
    }
}