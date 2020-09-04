package dev.gtcl.astro

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import dev.gtcl.astro.models.reddit.AccessToken
import dev.gtcl.astro.models.reddit.listing.Account

class AstroApplication : Application() {

    var accessToken: AccessToken? = null
        set(value){
            field = value
            Log.d("TOKEN", "Access Token: ${value?.authorizationHeader}")
        }

    var currentAccount: Account? = null

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val darkSettings = sharedPref.getString(DARK_SETTINGS_KEY, getString(R.string.use_system_settings))
        val allDarkSettings = resources.getStringArray(R.array.dark_mode_entries)
        when(allDarkSettings.indexOf(darkSettings)){
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}