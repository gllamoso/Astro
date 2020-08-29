package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.User

interface UserActions {
    fun viewProfile(user: User)
    fun message(user: User)
    fun remove(position: Int)
}