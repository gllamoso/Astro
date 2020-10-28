package dev.gtcl.astro.actions

import dev.gtcl.astro.models.reddit.listing.Item

interface ItemClickListener {
    fun clicked(item: Item, position: Int)
    fun longClicked(item: Item, position: Int)
}