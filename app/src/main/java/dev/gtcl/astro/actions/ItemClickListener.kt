package dev.gtcl.astro.actions

import dev.gtcl.astro.models.reddit.listing.Item

interface ItemClickListener {
    fun itemClicked(item: Item, position: Int)
}