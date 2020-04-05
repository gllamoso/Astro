package dev.gtcl.reddit.ui.fragments.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import dev.gtcl.reddit.POST_KEY
import dev.gtcl.reddit.databinding.FragmentShareOptionsBinding
import dev.gtcl.reddit.listings.Post

class ShareOptionsDialogFragment : DialogFragment(){

    private lateinit var binding: FragmentShareOptionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentShareOptionsBinding.inflate(inflater)
        val post = requireArguments().getParcelable(POST_KEY) as? Post
        binding.post = post
        binding.invalidateAll()
        return binding.root
    }
}