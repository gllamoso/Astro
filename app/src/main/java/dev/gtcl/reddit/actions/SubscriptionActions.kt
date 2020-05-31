package dev.gtcl.reddit.actions

import dev.gtcl.reddit.database.Subscription

interface SubscriptionActions{
    fun favorite(sub: Subscription, favorite: Boolean)
    fun remove(sub: Subscription)
    fun edit(sub: Subscription)
}