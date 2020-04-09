package dev.gtcl.reddit.ui.fragments.home.listing.subreddits

import dev.gtcl.reddit.listings.Subreddit

interface SubredditActions {
    fun addToFavorites(subreddit: Subreddit)
    fun subscribe(subreddit: Subreddit, subscribe: Boolean)
    fun fetchSubredditInfoThenSubscribe(srName: String)
}