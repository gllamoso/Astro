package dev.gtcl.reddit

import android.app.Application
import android.util.Log
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.models.reddit.AccessToken

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