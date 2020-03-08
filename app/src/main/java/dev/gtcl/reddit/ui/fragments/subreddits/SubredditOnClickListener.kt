package dev.gtcl.reddit.ui.fragments.subreddits

import dev.gtcl.reddit.posts.ListingType

interface SubredditOnClickListener{
    fun onClick(listing: ListingType)
}