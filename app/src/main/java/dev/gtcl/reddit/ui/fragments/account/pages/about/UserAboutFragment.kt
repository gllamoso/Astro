package dev.gtcl.reddit.ui.fragments.account.pages.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.USER_KEY
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentUserAboutBinding

class UserAboutFragment : Fragment() {

    private lateinit var binding: FragmentUserAboutBinding

    val model: UserAboutVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(UserAboutVM::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentUserAboutBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model
        val user = arguments?.getString(USER_KEY)
        model.fetchAccount(user)
        model.fetchAwards()

        val adapter = AwardsAdapter()
        model.awards.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })
        binding.awardsList.adapter = adapter
        return binding.root
    }

    companion object{
        fun newInstance(user: String?): UserAboutFragment{
            val fragment = UserAboutFragment()
            val args = bundleOf(USER_KEY to user)
            fragment.arguments = args
            return fragment
        }
    }
}