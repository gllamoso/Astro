package dev.gtcl.reddit

import android.app.Application
import android.util.Log
import dev.gtcl.reddit.listings.Account
import dev.gtcl.reddit.listings.users.AccessToken

class RedditApplication : Application() {

    var accessToken: AccessToken? = null
        set(value){
            field = value
            Log.d("TAE", "Access Token: ${value?.value}") // TODO: Remove
        }

    var currentAccount: Account? = null

    companion object{
        var accessToken2: AccessToken? = null
    }
}