package dev.gtcl.astro.ui.activities

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private lateinit var navController: NavController

    private val model: MainActivityVM by lazy {
        val viewModelFactory = ViewModelFactory(application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(MainActivityVM::class.java)
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
                binding?.root?.let {
                    Snackbar.make(it, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                model.errorMessageObserved()
            }
        })

        // Preemptively create external directories
        getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS)
    }

    override fun onResume() {
        super.onResume()
        if (!isChangingConfigurations) {
            val thisApplication = application as AstroApplication
            thisApplication.currentAccount?.let {
                model.refreshAccessToken()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Glide.get(this).clearMemory()
        binding = null
    }
}
