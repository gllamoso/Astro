package dev.gtcl.reddit.ui.activities.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.gson.Gson
import dev.gtcl.reddit.*
import dev.gtcl.reddit.database.asAccountDomainModel
import dev.gtcl.reddit.databinding.ActivityMainBinding
import dev.gtcl.reddit.databinding.LayoutNavHeaderBinding
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.ui.activities.signin.SignInActivity

class MainActivity : FragmentActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    val model: MainActivityViewModel by lazy {
        val viewModelFactory = ViewModelFactory(application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MainActivityViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        navController = findNavController(R.id.nav_host_fragment)
        getUserFromSharedPreferences()
        setDrawer(LayoutInflater.from(this))
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),1) // TODO: Move somewhere

//        val myUrl = "https://giant.gfycat.com/OffbeatRigidCivet.mp4" // WORKS!!!!
//        val myUrl = "https://v.redd.it/sd85fiv3vjt41/HLSPlaylist.m3u8"
//        val request = DownloadManager.Request(Uri.parse(myUrl))
//        request.apply {
//            setTitle("Download")
//            setDescription("Your file is downloading...")
//            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
//            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//            setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOWNLOADS, "HLSPlaylist.m3u8")
//        }
//        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//        manager.enqueue(request)
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
            val user = Gson().fromJson(it, Account::class.java)
            model.setCurrentUser(user, false)
        }
    }

    @SuppressLint("WrongConstant")
    private fun setDrawer(inflater: LayoutInflater){
        val drawerLayout = binding.drawerLayout
        val header = LayoutNavHeaderBinding.inflate(inflater)

        binding.expandableListView.addHeaderView(header.root)

        val adapter = MainDrawerAdapter(this,
            object :
                DrawerOnClickListeners {
                override fun onAddAccountClicked() {
                    startSignInActivity()
                }

                override fun onRemoveAccountClicked(username: String) {
                    model.deleteUserFromDatabase(username)
                }

                override fun onAccountClicked(account: Account) {
                    model.setCurrentUser(account, true)
                    drawerLayout.closeDrawer(Gravity.START)
                }

                override fun onLogoutClicked() {
                    model.setCurrentUser(null, true)
                    drawerLayout.closeDrawer(Gravity.START)
                }

                override fun onHomeClicked() {
                    navController.popBackStack(R.id.home_fragment, false)
                    drawerLayout.closeDrawer(Gravity.START)
                }

                override fun onMyAccountClicked() {
                    navController.navigate(R.id.account_fragment)
                    drawerLayout.closeDrawer(Gravity.START)
                }

                override fun onSettingsClicked() {
                    Toast.makeText(baseContext, "Settings", Toast.LENGTH_LONG).show()
                    drawerLayout.closeDrawer(Gravity.START)
                }

            })

        binding.expandableListView.setAdapter(adapter)

        model.allUsers.observe(this, Observer {
            adapter.setUsers(it.asAccountDomainModel())
        })

        model.currentAccount.observe(this, Observer {
            header.account = it
        })

        model.openDrawer.observe(this, Observer {
            if(it == true) {
                drawerLayout.openDrawer(Gravity.START)
                model.openDrawerComplete()
            }
        })

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener{
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerClosed(drawerView: View) {
                binding.expandableListView.collapseGroup(0)
            }

            override fun onDrawerOpened(drawerView: View) {
                adapter.notifyDataSetInvalidated()
            }
        })

        model.allowDrawerSwipe.observe(this, Observer {
            if(it == true)
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            else if(it == false)
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        })
    }

    private fun startSignInActivity() {
        val url = String.format(getString(R.string.auth_url), getString(R.string.client_id), STATE, getString(R.string.redirect_uri))
        val intent = Intent(this, SignInActivity::class.java)
        intent.putExtra(URL_KEY, url)
        startActivityForResult(intent,
            REDIRECT_URL_REQUEST_CODE
        )
    }
}