package dev.gtcl.reddit.actions

import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.models.reddit.listing.Comment
import dev.gtcl.reddit.models.reddit.listing.Post

interface PostActions {
    fun vote(post: Post, vote: Vote)
    fun share(post: Post)
    fun viewProfile(post: Post)
    fun save(post: Post)
    fun subredditSelected(sub: String)
    fun hide(post: Post, position: Int)
    fun report(post: Post, position: Int)
    fun thumbnailClicked(post: Post, position: Int)
    fun edit(post: Post, position: Int)
    fun manage(post: Post, position: Int)
    fun delete(post: Post, position: Int)
}