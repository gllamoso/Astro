package dev.gtcl.astro

import android.app.Application
import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.models.reddit.AccessToken
import dev.gtcl.astro.models.reddit.listing.Account
import timber.log.Timber

class AstroApplication : Application() {

    val sharedPref: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private var _accessToken: AccessToken? = null
        set(value) {
            Timber.tag("TOKEN").d("Access Token: ${value?.authorizationHeader}")
            field = value
        }
    val accessToken: AccessToken?
        get() = _accessToken

    private var _currentAccount: Account? = null
    val currentAccount: Account?
        get() = _currentAccount

    private var lastTokenUpdate: Long = 0

    val needsTokenRefresh: Boolean
        get() {
            val currentTime = System.currentTimeMillis()
            return accessToken != null && currentTime >= (lastTokenUpdate + 1_800_000) // if it's been 30 mins since last token refresh
        }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val darkSettings =
            sharedPref.getString(DARK_SETTINGS_KEY, getString(R.string.use_system_settings))
        val allDarkSettings = resources.getStringArray(R.array.dark_mode_entries)
        when (allDarkSettings.indexOf(darkSettings)) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        _accessToken =
            Gson().fromJson(sharedPref.getString(ACCESS_TOKEN_KEY, null), AccessToken::class.java)
        _currentAccount =
            Gson().fromJson(sharedPref.getString(CURRENT_USER_KEY, null), Account::class.java)
        lastTokenUpdate = sharedPref.getLong(LAST_TOKEN_REFRESH_KEY, 0)
    }

    @MainThread
    fun setAccessToken(accessToken: AccessToken?) {
        _accessToken = accessToken
        lastTokenUpdate = System.currentTimeMillis()
        with(sharedPref.edit()) {
            val json = Gson().toJson(accessToken)
            putString(ACCESS_TOKEN_KEY, json)
            putLong(LAST_TOKEN_REFRESH_KEY, lastTokenUpdate)
            commit()
        }
    }

    @MainThread
    fun setCurrentAccount(account: Account?) {
        _currentAccount = account
        with(sharedPref.edit()) {
            val json = Gson().toJson(account)
            putString(CURRENT_USER_KEY, json)
            commit()
        }
    }

    @MainThread
    fun saveAccount(account: SavedAccount?) {
        with(sharedPref.edit()) {
            val json = Gson().toJson(account)
            putString(SAVED_ACCOUNT_KEY, json)
            commit()
        }
    }

    @MainThread
    fun getSavedAccount(): SavedAccount? {
        val accountString = sharedPref.getString(SAVED_ACCOUNT_KEY, null)
        return Gson().fromJson(accountString, SavedAccount::class.java)
    }
}