package dev.gtcl.astro.ui.fragments.account.pages.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.USER_KEY
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.databinding.FragmentAccountAboutBinding

class AccountAboutFragment : Fragment() {

    private var binding: FragmentAccountAboutBinding? = null

    val model: AccountAboutVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(AccountAboutVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountAboutBinding.inflate(inflater)
        binding?.lifecycleOwner = this
        binding?.model = model
        val user = arguments?.getString(USER_KEY)
        model.fetchAccount(user)
        if ((requireActivity().application as AstroApplication).currentAccount != null) {
            model.fetchAwards()
        }

        val adapter = AwardsAdapter()
        model.awards.observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })

        model.errorMessage.observe(viewLifecycleOwner, {
            if (it != null) {
                Snackbar.make(binding!!.root, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        binding?.fragmentAccountAboutAwardsList?.adapter = adapter
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Glide.get(requireContext()).clearMemory()
        binding = null
    }

    companion object {
        fun newInstance(user: String?): AccountAboutFragment {
            val fragment = AccountAboutFragment()
            val args = bundleOf(USER_KEY to user)
            fragment.arguments = args
            return fragment
        }
    }
}