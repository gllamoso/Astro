package dev.gtcl.reddit.actions

import dev.gtcl.reddit.models.reddit.User
import dev.gtcl.reddit.models.reddit.UserType

interface UserActions {
    fun viewProfile(user: User)
    fun message(user: User)
    fun remove(position: Int)
}