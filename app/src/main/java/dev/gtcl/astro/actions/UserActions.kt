package dev.gtcl.astro.actions

import dev.gtcl.astro.models.reddit.User

interface UserActions {
    fun viewProfile(user: User)
    fun message(user: User)
    fun remove(position: Int)
}