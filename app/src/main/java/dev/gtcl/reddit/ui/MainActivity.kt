package dev.gtcl.reddit.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.plusAssign
import androidx.navigation.ui.NavigationUI
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.ActivityMainBinding
import dev.gtcl.reddit.databinding.NavHeader2Binding
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.ui.webview.WebviewActivity
import dev.gtcl.reddit.users.User
import dev.gtcl.reddit.database.asDomainModel

const val URL_KEY = "URL"
const val REDIRECT_URL_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var drawerToggle: ActionBarDrawerToggle

    val model: MainActivityViewModel by lazy {
        val viewModelFactory = MainActivityViewModelFactory(application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MainActivityViewModel::class.java)
    }

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        drawerLayout = binding.drawerLayout
        navController = findNavController(R.id.nav_host_fragment)

        getUserFromSharedPreferences()
        model.getPosts(Subreddit(displayName = "funny"))
        setExpandableListView()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            drawerLayout.setDrawerLockMode(
                if(destination.id == R.id.postListFragment) DrawerLayout.LOCK_MODE_UNLOCKED
                else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            )
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, drawerLayout) || super.onSupportNavigateUp()
    }

    fun selectSubreddit(subreddit: Subreddit){
        model.getPosts(subreddit)
    }

    override fun onBackPressed() {
        if(!navController.popBackStack())
            super.onBackPressed()
    }

    private fun setExpandableListView() {
        val listHeaderView = NavHeader2Binding.inflate(layoutInflater)

        model.currentUser.observe(this, Observer {
            if(it != null)
                Log.d("TAE", "Current user: $it")
            listHeaderView.user = it
        })

        binding.expandableListView.addHeaderView(listHeaderView.root)

        val adapter = CustomExpandableListAdapter(this, object : AdapterOnClickListeners{
            override fun onAddAccountClicked() {
                signInUser()
            }

            override fun onRemoveAccountClicked(username: String) {
                model.deleteUserFromDatabase(username)
            }

            override fun onAccountClicked(user: User) {
                model.setCurrentUser(user)
                model.renewAccessToken()
            }

            override fun onLogoutClicked() {
                model.setCurrentUser(null)
            }
        })
        binding.expandableListView.setAdapter(adapter)

        drawerToggle = object: ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close){
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                invalidateOptionsMenu()
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                invalidateOptionsMenu()
            }
        }
        drawerToggle.isDrawerIndicatorEnabled = true
        drawerLayout.addDrawerListener(drawerToggle)

        model.allUsers.observe(this, Observer {
            adapter.setUsers(it.asDomainModel())
        })
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
        startActivityForResult(intent, REDIRECT_URL_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == REDIRECT_URL_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val uri = data?.data
            uri?.let { model.getCodeFromUri(it) }
        }
    }

    @SuppressLint("WrongConstant")
    fun showDrawer(){
        drawerLayout.openDrawer(Gravity.START)
    }

    fun navigateUp(){
        navController.navigateUp()
    }
}
