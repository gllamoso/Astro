package dev.gtcl.astro.actions

import dev.gtcl.astro.Vote
import dev.gtcl.astro.models.reddit.listing.Comment

interface CommentActions {
    fun vote(comment: Comment, vote: Vote)
    fun save(comment: Comment)
    fun share(comment: Comment)
    fun reply(comment: Comment, position: Int)
    fun viewProfile(comment: Comment)
    fun report(comment: Comment, position: Int)
    fun edit(comment: Comment, position: Int)
    fun delete(comment: Comment, position: Int)
}