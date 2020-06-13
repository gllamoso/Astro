package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.listing.Subreddit

interface SubredditActions{
    fun subscribe(subreddit: Subreddit, subscribe: Boolean)
}