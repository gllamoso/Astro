package dev.gtcl.reddit.ui.fragments.account.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentUserAboutBinding

class UserAboutFragment : Fragment() {

    private lateinit var binding: FragmentUserAboutBinding

    val model: UserAboutViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(UserAboutViewModel::class.java)
    }

    fun setUser(user: String?){
        model.fetchAccount(user)
        model.fetchAwards()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentUserAboutBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model
        return binding.root
    }
}