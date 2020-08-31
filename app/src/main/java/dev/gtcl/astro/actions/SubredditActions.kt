package dev.gtcl.astro.actions

import dev.gtcl.astro.models.reddit.listing.Subreddit

interface SubredditActions{
    fun subscribe(subreddit: Subreddit, subscribe: Boolean)
}