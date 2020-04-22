package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.Subreddit

interface SubredditActions{
    fun onClick(subreddit: Subreddit)
    fun addToFavorites(subreddit: Subreddit, favorite: Boolean, refresh: Boolean)
    fun subscribe(subreddit: Subreddit, subscribe: Boolean, refresh: Boolean)
}