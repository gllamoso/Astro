package dev.gtcl.reddit.ui.fragments.account.user.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.gtcl.reddit.databinding.FragmentUserAboutBinding
import dev.gtcl.reddit.ui.fragments.account.user.UserFragment

class UserAboutFragment : Fragment() {

    private lateinit var binding: FragmentUserAboutBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentUserAboutBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = (parentFragment as UserFragment).model
        return binding.root
    }
}