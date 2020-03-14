package dev.gtcl.reddit.ui.fragments.posts

import dev.gtcl.reddit.network.Post

interface PostViewClickListener {
    fun onPostClicked(post: Post?, position: Int)
    fun onThumbnailClicked(post: Post)
}