package dev.gtcl.reddit.actions

import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.models.reddit.listing.Comment

interface CommentActions {
    fun vote(comment: Comment, vote: Vote)
    fun save(comment: Comment)
    fun share(comment: Comment)
    fun reply(comment: Comment)
    fun viewProfile(comment: Comment)
    fun report(comment: Comment)
}