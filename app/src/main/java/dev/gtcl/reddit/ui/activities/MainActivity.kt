package dev.gtcl.reddit.ui.activities

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.ActivityMainBinding

class MainActivity : FragmentActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val model: MainActivityVM by lazy {
        val viewModelFactory = ViewModelFactory(application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MainActivityVM::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.nav_host_fragment)
        model.openChromeTab.observe(this, Observer {
            if (it != null) {
                val url = if(it.startsWith("/")){
                    "https://www.reddit.com${it}"
                } else {
                    it
                }
                val intent = CustomTabsIntent.Builder().apply {
//                  TODO: Add Animations
//                    setStartAnimations(this@MainActivity, R.anim.slide_right, R.anim.slide_right)
//                    setExitAnimations(this@MainActivity, R.anim.slide_left, R.anim.slide_left)
                }.build()
                intent.launchUrl(this, Uri.parse(url))
                model.chromeTabOpened()
            }
        })
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            1
        ) // TODO: Move somewhere
    }

    override fun onResume() {
        super.onResume()
        if (!isChangingConfigurations) {
            val thisApplication = application as RedditApplication
            thisApplication.currentAccount?.let {
                model.refreshAccessToken()
            }
        }
    }
}
