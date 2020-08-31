package dev.gtcl.astro.ui.activities

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.databinding.ActivityMainBinding

class MainActivity : FragmentActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val model: MainActivityVM by lazy {
        val viewModelFactory = ViewModelFactory(application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(MainActivityVM::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.activityMain_navHostFragment)
        model.openChromeTab.observe(this, {
            if (it != null) {
                val url = if(it.startsWith("/")){
                    "https://www.reddit.com${it}"
                } else {
                    it
                }
                val intent = CustomTabsIntent.Builder().build()
                intent.launchUrl(this, Uri.parse(url))
                model.chromeTabOpened()
            }
        })

        model.errorMessage.observe(this, {
            if(it != null){
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
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
}
