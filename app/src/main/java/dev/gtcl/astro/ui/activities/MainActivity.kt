package dev.gtcl.astro.ui.activities

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.R
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private lateinit var navController: NavController

    private val application by lazy {
        getApplication() as AstroApplication
    }

    private val model: MainActivityVM by lazy {
        val viewModelFactory = ViewModelFactory(application)
        ViewModelProvider(this, viewModelFactory).get(MainActivityVM::class.java)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (application.needsTokenRefresh) {
                model.refreshAccessToken()
            }
        }
    }

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val isNetworkAvailable: Boolean
        get() {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val activeNetwork =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        supportActionBar?.hide()
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.activityMain_navHostFragment) as NavHostFragment
        navController = navHostFragment.navController
        model.openChromeTab.observe(this, {
            if (it != null) {
                val url = if (it.startsWith("/")) {
                    "https://www.reddit.com${it}"
                } else {
                    it
                }
                val intent = CustomTabsIntent.Builder().build()
                intent.launchUrl(this, Uri.parse(url))
                model.chromeTabOpened()
            }
        })

        model.errorMessage.observe(this, { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(baseContext, errorMessage, Toast.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        model.mediaDialogOpened.observe(this, {
            if (it) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        })

        // Preemptively create external directories
        getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS)

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    override fun onResume() {
        super.onResume()
        if (application.needsTokenRefresh && isNetworkAvailable) {
            model.refreshAccessToken()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
