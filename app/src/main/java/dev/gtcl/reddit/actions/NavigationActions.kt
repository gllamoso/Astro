package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.listing.ListingType

interface NavigationActions {
    fun listingSelected(listing: ListingType)
    fun accountSelected(user: String?)
    fun messagesSelected()
    fun signInNewAccount()
    fun launchWebview(url: String)
}