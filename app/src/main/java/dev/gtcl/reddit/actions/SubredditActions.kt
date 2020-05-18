package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.Subreddit

interface SubredditActions{
    fun favorite(subreddit: Subreddit, favorite: Boolean)
    fun subscribe(subreddit: Subreddit, subscribe: Boolean)
}