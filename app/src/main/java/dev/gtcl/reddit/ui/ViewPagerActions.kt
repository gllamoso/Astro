package dev.gtcl.reddit.ui

import dev.gtcl.reddit.listings.Comment
import dev.gtcl.reddit.listings.Post

interface ViewPagerActions{
    fun enablePagerSwiping(enable: Boolean)
    fun navigatePrevious()
    fun viewComments(post: Post)
    fun viewComments(comment: Comment)
}