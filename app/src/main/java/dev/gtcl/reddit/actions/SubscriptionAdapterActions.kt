package dev.gtcl.reddit.actions

import dev.gtcl.reddit.database.Subscription

interface SubscriptionAdapterActions {
    fun addToFavorites(sub: Subscription)
    fun removeFromFavorites(sub: Subscription, updateOtherSections: Boolean)
    fun remove(sub: Subscription)
}