package dev.gtcl.reddit.actions

import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.models.reddit.Post

interface PostActions {
    fun vote(post: Post, vote: Vote)
    fun share(post: Post)
    fun viewProfile(post: Post)
    fun save(post: Post)
    fun hide(post: Post, position: Int)
    fun report(post: Post)
    fun thumbnailClicked(post: Post)
}