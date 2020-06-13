package dev.gtcl.reddit.ui.fragments.splash

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import dev.gtcl.reddit.CURRENT_USER_KEY
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentSplashBinding
import dev.gtcl.reddit.models.reddit.listing.Account
import dev.gtcl.reddit.models.reddit.listing.FrontPage
import dev.gtcl.reddit.ui.fragments.ListingPage

class SplashFragment : Fragment(){

    private lateinit var binding: FragmentSplashBinding

    private val model: SplashVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SplashVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSplashBinding.inflate(inflater)
        getUserFromSharedPreferences()

        model.ready.observe(viewLifecycleOwner, Observer {
            if(it != null){
                findNavController().navigate(SplashFragmentDirections.actionSplashScreenFragmentToViewPagerFragment(ListingPage(
                    FrontPage
                )))
                model.readyComplete()
            }
        })

        return binding.root
    }

    private fun getUserFromSharedPreferences(){
        val sharedPref = requireActivity().getSharedPreferences(getString(R.string.preferences_file_key), Context.MODE_PRIVATE)
        val userString = sharedPref.getString(CURRENT_USER_KEY, null)
        val account = Gson().fromJson(userString, Account::class.java)
        model.setCurrentUser(account, false)
    }
}