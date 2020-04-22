package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.Comment
import dev.gtcl.reddit.models.reddit.Post

interface ViewPagerActions{
    fun enablePagerSwiping(enable: Boolean)
    fun navigatePrevious()
    fun viewComments(post: Post)
    fun viewComments(comment: Comment)
}