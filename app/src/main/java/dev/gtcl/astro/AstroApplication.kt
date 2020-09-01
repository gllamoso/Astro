package dev.gtcl.astro

import android.app.Application
import android.util.Log
import dev.gtcl.astro.models.reddit.AccessToken
import dev.gtcl.astro.models.reddit.listing.Account

class AstroApplication : Application() {

    var accessToken: AccessToken? = null
        set(value){
            field = value
            Log.d("TOKEN", "Access Token: ${value?.authorizationHeader}")
        }

    var currentAccount: Account? = null
}