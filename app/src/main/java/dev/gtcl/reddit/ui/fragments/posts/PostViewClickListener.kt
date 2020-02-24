package dev.gtcl.reddit.ui.fragments.posts

import dev.gtcl.reddit.posts.Post

interface PostViewClickListener {
    fun onPostClicked(post: Post?, position: Int)
    fun onThumbnailClicked(post: Post)
}