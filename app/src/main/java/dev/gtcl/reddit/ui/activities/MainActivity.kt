package dev.gtcl.reddit.ui.activities

import android.Manifest
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
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
        val toInitialize = model
//        getUserFromSharedPreferences()
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),1) // TODO: Move somewhere
    }
}
