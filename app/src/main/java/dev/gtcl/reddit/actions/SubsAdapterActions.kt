package dev.gtcl.reddit.actions

import dev.gtcl.reddit.listings.Subreddit

interface SubsAdapterActions {
    fun addToFavorites(subreddit: Subreddit)
    fun removeFromFavorites(subreddit: Subreddit)
    fun remove(subreddit: Subreddit)
}