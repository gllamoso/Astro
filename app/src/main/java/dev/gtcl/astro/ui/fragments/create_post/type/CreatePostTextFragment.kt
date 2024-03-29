package dev.gtcl.astro.ui.fragments.create_post.type

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import dev.gtcl.astro.databinding.FragmentCreatePostTextBinding
import dev.gtcl.astro.ui.fragments.create_post.CreatePostVM
import dev.gtcl.astro.ui.fragments.create_post.TextPost

class CreatePostTextFragment : Fragment() {

    private var binding: FragmentCreatePostTextBinding? = null

    val model: CreatePostVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(CreatePostVM::class.java)
    }

    override fun onResume() {
        super.onResume()
        initObservers()
    }

    override fun onPause() {
        super.onPause()
        removeObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreatePostTextBinding.inflate(inflater)
        binding?.lifecycleOwner = this
        initObservers()
        return binding?.root
    }

    private fun initObservers() {
        model.fetchInput.observe(viewLifecycleOwner, {
            if (it == true) {
                model.setPostContent(TextPost(binding?.fragmentCreatePostTextText?.text.toString()))
                model.dataFetched()
            }
        })

        binding?.fragmentCreatePostTextText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding?.fragmentCreatePostTextTextInputLayout?.error = null
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun removeObservers() {
        model.fetchInput.removeObservers(viewLifecycleOwner)
    }
}