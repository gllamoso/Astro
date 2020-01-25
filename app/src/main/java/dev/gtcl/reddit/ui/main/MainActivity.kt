package dev.gtcl.reddit.ui.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.ActivityMainBinding
import dev.gtcl.reddit.ui.webview.WebviewActivity
import dev.gtcl.reddit.users.User

const val URL_KEY = "URL"
const val REDIRECT_URL_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    val model: MainActivityViewModel by lazy {
        val viewModelFactory =
            MainActivityViewModelFactory(
                application as RedditApplication
            )
        ViewModelProvider(this, viewModelFactory).get(MainActivityViewModel::class.java)
    }

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.nav_host_fragment)
        getUserFromSharedPreferences()
    }

    override fun onBackPressed() {
        if(!navController.popBackStack())
            super.onBackPressed()
    }

    private fun getUserFromSharedPreferences(){
        val sharedPref = this.getSharedPreferences(getString(R.string.preferences_file_key), Context.MODE_PRIVATE)
        val userString = sharedPref.getString(getString(R.string.current_user_key), null)
        userString?.let {
            val user = Gson().fromJson<User>(it, User::class.java)
            model.setCurrentUser(user)
        }
    }

    private fun signInUser() {
        val url = String.format(getString(R.string.auth_url), getString(R.string.client_id), STATE, getString(R.string.redirect_uri))
        val intent = Intent(this, WebviewActivity::class.java)
        intent.putExtra(URL_KEY, url)
        startActivityForResult(intent,
            REDIRECT_URL_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REDIRECT_URL_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val uri = data?.data
            uri?.let { model.getCodeFromUri(it) }
        }
    }

    fun navigateUp(){
        navController.navigateUp()
    }
}
