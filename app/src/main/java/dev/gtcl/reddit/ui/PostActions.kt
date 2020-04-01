package dev.gtcl.reddit.ui

import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.listings.Post

interface PostActions {
    fun vote(post: Post, vote: Vote)
    fun share(post: Post)
    fun award(post: Post)
    fun save(post: Post)
    fun hide(post: Post)
    fun report(post: Post)
    fun postClicked(post: Post)
    fun thumbnailClicked(post: Post)
}