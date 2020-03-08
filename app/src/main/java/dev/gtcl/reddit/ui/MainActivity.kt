package dev.gtcl.reddit.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.ActivityMainBinding
import dev.gtcl.reddit.users.User

const val URL_KEY = "URL"
const val REDIRECT_URL_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    val model: MainActivityViewModel by lazy {
        val viewModelFactory = MainActivityViewModelFactory(application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MainActivityViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.nav_host_fragment)
        getUserFromSharedPreferences()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REDIRECT_URL_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val uri = data?.data
            uri?.let { model.fetchUserFromUri(it) }
        }
    }

    override fun onBackPressed() {
        if(!navController.popBackStack())
            super.onBackPressed()
    }

    // ---- HELPER FUNCTIONS -------

    private fun getUserFromSharedPreferences(){
        val sharedPref = this.getSharedPreferences(getString(R.string.preferences_file_key), Context.MODE_PRIVATE)
        val userString = sharedPref.getString(getString(R.string.current_user_key), null)
        userString?.let {
            val user = Gson().fromJson(it, User::class.java)
            model.setCurrentUser(user, false)
        }
    }

    fun navigateUp(){
        navController.navigateUp()
    }
}
