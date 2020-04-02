package dev.gtcl.reddit

import android.app.Application
import android.util.Log
import dev.gtcl.reddit.listings.users.AccessToken
import dev.gtcl.reddit.listings.users.User

class RedditApplication : Application() {

    var accessToken: AccessToken? = null
        set(value){
            field = value
            Log.d("TAE", "Access Token: ${value?.value}") // TODO: Remove
        }

    var currentUser: User? = null

    companion object{
        var accessToken2: AccessToken? = null
    }
}