package dev.gtcl.reddit

import android.app.Application
import android.util.Log
import dev.gtcl.reddit.models.reddit.AccessToken
import dev.gtcl.reddit.models.reddit.listing.Account

class RedditApplication : Application() {

    var accessToken: AccessToken? = null
        set(value){
            field = value
            Log.d("TOKEN", "Access Token: ${value?.authorizationHeader}") // TODO: Remove
        }

    var currentAccount: Account? = null

}