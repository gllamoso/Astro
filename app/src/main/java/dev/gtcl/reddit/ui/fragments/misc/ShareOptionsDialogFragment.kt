package dev.gtcl.reddit.ui.fragments.misc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import dev.gtcl.reddit.POST_KEY
import dev.gtcl.reddit.databinding.FragmentShareOptionsBinding
import dev.gtcl.reddit.models.reddit.Post

class ShareOptionsDialogFragment : DialogFragment(){

    private lateinit var binding: FragmentShareOptionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentShareOptionsBinding.inflate(inflater)
        val post = requireArguments().getParcelable(POST_KEY) as? Post
        binding.post = post
        binding.invalidateAll()
        return binding.root
    }

    companion object{
        fun newInstance(post: Post): ShareOptionsDialogFragment{
            val fragment = ShareOptionsDialogFragment()
            val args = bundleOf(POST_KEY to post)
            fragment.arguments = args
            return fragment
        }
    }
}