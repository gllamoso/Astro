package dev.gtcl.reddit

import android.app.Application
import android.util.Log
import dev.gtcl.reddit.listings.ListingRepository
import dev.gtcl.reddit.listings.getPostRepository
import dev.gtcl.reddit.listings.subs.SubredditRepository
import dev.gtcl.reddit.listings.subs.getSubredditRepository
import dev.gtcl.reddit.listings.users.AccessToken
import dev.gtcl.reddit.listings.users.User
import dev.gtcl.reddit.listings.users.UserRepository
import dev.gtcl.reddit.listings.users.getUserRepository
import java.util.concurrent.Executors

class RedditApplication : Application() {

    val userRepository: UserRepository by lazy {
        getUserRepository(this)
    }

    val listingRepository: ListingRepository by lazy {
        getPostRepository(this, Executors.newFixedThreadPool(5))
    }

    val subredditRepository: SubredditRepository by lazy {
        getSubredditRepository(Executors.newFixedThreadPool(5))
    }

    var accessToken: AccessToken? = null
        set(value){
            field = value
            Log.d("TAE", "Access Token: ${value?.value}") // TODO: Remove
        }

    var currentUser: User? = null


}