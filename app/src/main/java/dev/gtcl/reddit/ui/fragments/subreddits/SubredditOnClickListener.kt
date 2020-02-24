package dev.gtcl.reddit.ui.fragments.subreddits

import dev.gtcl.reddit.subs.Subreddit

interface SubredditOnClickListener{
    fun onClick(sub: Subreddit)
}