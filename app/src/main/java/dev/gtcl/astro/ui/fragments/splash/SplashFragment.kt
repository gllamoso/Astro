package dev.gtcl.astro.ui.fragments.splash

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import dev.gtcl.astro.*
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.databinding.FragmentSplashBinding
import dev.gtcl.astro.models.reddit.listing.FrontPage
import dev.gtcl.astro.ui.fragments.ListingPage

class SplashFragment : Fragment(){

    private lateinit var binding: FragmentSplashBinding

    private val model: SplashVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(SplashVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSplashBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = viewLifecycleOwner
        setUserFromSharedPreferences()

        model.ready.observe(viewLifecycleOwner, {
            if(it != null){
                findNavController().navigate(SplashFragmentDirections.actionSplashScreenFragmentToViewPagerFragment(ListingPage(
                    FrontPage
                )))
                model.readyComplete()
            }
        })

        binding.fragmentSplashRetryButton.setOnClickListener {
            model.errorMessageObserved()
            setUserFromSharedPreferences()
        }

        return binding.root
    }

    private fun setUserFromSharedPreferences(){
        val sharedPref = requireActivity().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        val userString = sharedPref.getString(CURRENT_USER_KEY, null)
        val account = Gson().fromJson(userString, SavedAccount::class.java)
        model.setCurrentUser(account, false)
    }
}