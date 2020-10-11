package dev.gtcl.astro.ui.fragments.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import dev.gtcl.astro.*
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.databinding.FragmentSplashBinding
import dev.gtcl.astro.models.reddit.listing.FrontPage
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.view_pager.ListingPage

class SplashFragment : Fragment() {

    private var binding: FragmentSplashBinding? = null

    private val model: SplashVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(SplashVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private val application by lazy {
        requireActivity().application as AstroApplication
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSplashBinding.inflate(inflater)
        binding?.model = model
        binding?.lifecycleOwner = viewLifecycleOwner
        setUserFromSharedPreferences()

        model.ready.observe(viewLifecycleOwner, {
            if (it != null) {
                findNavController().navigate(
                    SplashFragmentDirections.actionSplashScreenFragmentToViewPagerFragment(
                        ListingPage(FrontPage)
                    )
                )
                model.readyObserved()
                activityModel.syncSubscriptionsWithReddit()
            }
        })

        binding?.fragmentSplashRetryButton?.setOnClickListener {
            model.errorMessageObserved()
            setUserFromSharedPreferences()
        }

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setUserFromSharedPreferences() {
        val account = application.getSavedAccount()
        model.setCurrentUser(account)
    }
}