package dev.gtcl.reddit.actions

import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.Time

interface SortActions {
    fun sortSelected(sort: PostSort, time: Time?)
}