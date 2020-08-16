package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.listing.Listing

interface NavigationActions {
    fun listingSelected(listing: Listing)
    fun accountSelected(user: String?)
    fun messagesSelected()
    fun signInNewAccount()
    fun launchWebview(url: String)
}