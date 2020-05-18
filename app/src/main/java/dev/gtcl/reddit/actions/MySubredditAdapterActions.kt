package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.Subreddit

interface MySubredditAdapterActions {
    fun addToFavorites(sub: Subreddit)
    fun removeFromFavorites(sub: Subreddit, updateAllSubredditsSection: Boolean)
    fun remove(sub: Subreddit)
}