package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.listing.Item

interface ViewPagerActions{
    fun enablePagerSwiping(enable: Boolean)
    fun navigatePreviousPage()
    fun navigateToNewPage(item: Item)
}