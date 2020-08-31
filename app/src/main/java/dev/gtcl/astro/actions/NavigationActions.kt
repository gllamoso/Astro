package dev.gtcl.astro.actions

import dev.gtcl.astro.models.reddit.listing.Listing

interface NavigationActions {
    fun listingSelected(listing: Listing)
    fun accountSelected(user: String?)
    fun messagesSelected()
    fun signInNewAccount()
    fun launchWebview(url: String)
}