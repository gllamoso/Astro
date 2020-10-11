package dev.gtcl.astro.repositories.reddit

import dev.gtcl.astro.NotLoggedInException
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.CommentSort
import dev.gtcl.astro.Vote
import dev.gtcl.astro.database.ItemRead
import dev.gtcl.astro.database.redditDatabase
import dev.gtcl.astro.models.reddit.MoreChildrenResponse
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.CommentPage
import dev.gtcl.astro.network.RedditApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class MiscRepository private constructor(private val application: AstroApplication) {

    private val database = redditDatabase(application)

    fun vote(fullname: String, vote: Vote): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.vote(
            application.accessToken!!.authorizationHeader,
            fullname,
            vote.value
        )
    }

    fun save(id: String): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.save(application.accessToken!!.authorizationHeader, id)
    }

    fun unsave(id: String): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.unsave(application.accessToken!!.authorizationHeader, id)
    }

    fun hide(id: String): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.hide(application.accessToken!!.authorizationHeader, id)
    }

    fun unhide(id: String): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.unhide(application.accessToken!!.authorizationHeader, id)
    }

    fun report(
        id: String,
        ruleReason: String? = null,
        siteReason: String? = null,
        otherReason: String? = null
    ): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.report(
            application.accessToken!!.authorizationHeader,
            id,
            ruleReason,
            siteReason,
            otherReason
        )
    }

    fun markNsfw(id: String, nsfw: Boolean): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return if (nsfw) {
            RedditApi.oauth.markNsfw(application.accessToken!!.authorizationHeader, id)
        } else {
            RedditApi.oauth.unmarkNsfw(application.accessToken!!.authorizationHeader, id)
        }
    }

    fun markSpoiler(id: String, spoiler: Boolean): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return if (spoiler) {
            RedditApi.oauth.markSpoiler(application.accessToken!!.authorizationHeader, id)
        } else {
            RedditApi.oauth.unmarkSpoiler(application.accessToken!!.authorizationHeader, id)
        }
    }

    fun sendRepliesToInbox(id: String, state: Boolean): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.setSendReplies(
            application.accessToken!!.authorizationHeader,
            id,
            state
        )
    }

    fun setFlair(id: String, flair: Flair?): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.setFlair(
            application.accessToken!!.authorizationHeader,
            id,
            flair?.text,
            flair?.id
        )
    }

    fun delete(thingId: String): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.deleteThing(application.accessToken!!.authorizationHeader, thingId)
    }

    fun getAwards(user: String): Deferred<TrophyListingResponse> {
        return if (application.accessToken == null) RedditApi.base.getAwards(null, user)
        else RedditApi.oauth.getAwards(application.accessToken!!.authorizationHeader, user)
    }

    fun editText(thingId: String, text: String): Deferred<MoreChildrenResponse> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.editText(
            application.accessToken!!.authorizationHeader,
            thingId,
            text
        )
    }

    fun sendMessage(to: String, subject: String, markdown: String): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.sendMessage(
            application.accessToken!!.authorizationHeader,
            to,
            subject,
            markdown
        )
    }

    fun addComment(parentName: String, body: String): Deferred<MoreChildrenResponse> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.addComment(
            application.accessToken!!.authorizationHeader,
            parentName,
            body
        )
    }

    fun block(fullId: String): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.blockMessage(application.accessToken!!.authorizationHeader, fullId)
    }

    fun markMessage(item: Item, read: Boolean): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return if (read) {
            RedditApi.oauth.readMessage(application.accessToken!!.authorizationHeader, item.name)
        } else {
            RedditApi.oauth.unreadMessage(application.accessToken!!.authorizationHeader, item.name)
        }
    }

    fun deleteMessage(message: Message): Deferred<Response<Unit>> {
        if (application.accessToken == null) {
            throw NotLoggedInException()
        }
        return RedditApi.oauth.deleteMessage(
            application.accessToken!!.authorizationHeader,
            message.name
        )
    }

    suspend fun getReadPosts() = database.readItemDao.getAll()

    suspend fun addReadItem(item: Item) {
        withContext(Dispatchers.IO) {
            database.readItemDao.insert(ItemRead(item.name))
        }
    }

    // --- COMMENTS
    fun getPostAndComments(
        permalink: String,
        sort: CommentSort = CommentSort.BEST,
        limit: Int = 15
    ): Deferred<CommentPage> {
        val linkWithoutDomain = permalink.replace("http[s]?://www\\.reddit\\.com/".toRegex(), "")
        return if (application.accessToken == null) {
            RedditApi.base.getPostAndComments(null, "$linkWithoutDomain.json", sort, limit)
        } else {
            RedditApi.oauth.getPostAndComments(
                application.accessToken!!.authorizationHeader,
                "$linkWithoutDomain.json",
                sort,
                limit
            )
        }
    }

    fun getMoreComments(
        children: String,
        linkId: String,
        sort: CommentSort = CommentSort.BEST
    ): Deferred<MoreChildrenResponse> {
        return if (application.accessToken == null) {
            RedditApi.base.getMoreComments(null, children, linkId, sort = sort)
        } else {
            RedditApi.oauth.getMoreComments(
                application.accessToken!!.authorizationHeader,
                children,
                linkId,
                sort = sort
            )
        }
    }

    companion object {
        private lateinit var INSTANCE: MiscRepository
        fun getInstance(application: AstroApplication): MiscRepository {
            if (!Companion::INSTANCE.isInitialized) {
                INSTANCE = MiscRepository(application)
            }
            return INSTANCE
        }
    }
}