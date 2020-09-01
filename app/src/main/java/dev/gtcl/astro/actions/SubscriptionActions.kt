package dev.gtcl.astro.actions

import dev.gtcl.astro.database.Subscription

interface SubscriptionActions{
    fun favorite(sub: Subscription, favorite: Boolean, inFavoritesSection: Boolean)
    fun remove(sub: Subscription)
    fun editMultiReddit(sub: Subscription)
}