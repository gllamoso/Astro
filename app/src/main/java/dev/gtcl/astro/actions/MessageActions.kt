package dev.gtcl.astro.actions

import dev.gtcl.astro.models.reddit.listing.Message

interface MessageActions {
    fun reply(message: Message)
    fun mark(message: Message, read: Boolean)
    fun delete(message: Message, position: Int)
    fun viewProfile(message: Message)
    fun block(message: Message, position: Int)
}