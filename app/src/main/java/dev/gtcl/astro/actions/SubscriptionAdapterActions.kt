package dev.gtcl.astro.actions

import dev.gtcl.astro.database.Subscription

interface SubscriptionAdapterActions {
    fun addToFavorites(sub: Subscription)
    fun removeFromFavorites(sub: Subscription, updateOtherSections: Boolean)
    fun remove(sub: Subscription)
}