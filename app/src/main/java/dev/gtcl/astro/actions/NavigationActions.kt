package dev.gtcl.astro.actions

import dev.gtcl.astro.models.reddit.listing.PostListing

interface NavigationActions {
    fun listingSelected(postListing: PostListing)
    fun accountSelected(user: String?)
    fun messagesSelected()
    fun signInNewAccount()
    fun launchWebview(url: String)
}