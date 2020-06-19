package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.listing.Item

interface ItemClickListener {
    fun itemClicked(item: Item, position: Int)
}