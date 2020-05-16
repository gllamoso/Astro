package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.Item

interface ItemClickListener {
    fun itemClicked(item: Item)
}