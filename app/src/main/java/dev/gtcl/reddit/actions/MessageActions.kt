package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.listing.Message

interface MessageActions {
    fun reply(message: Message)
    fun mark(message: Message)
    fun delete(message: Message)
    fun viewProfile(user: String)
    fun block(user: String)
}