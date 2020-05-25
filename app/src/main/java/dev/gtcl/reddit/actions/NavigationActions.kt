package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.ListingType

interface NavigationActions {
    fun listingSelected(listing: ListingType)
    fun accountSelected(user: String?)
    fun messagesSelected()
}