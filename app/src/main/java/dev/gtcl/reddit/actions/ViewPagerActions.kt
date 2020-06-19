package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.listing.Post


interface ViewPagerActions{
    fun enablePagerSwiping(enable: Boolean)
    fun navigatePreviousPage()
    fun navigateToComments(post: Post, position: Int)
}