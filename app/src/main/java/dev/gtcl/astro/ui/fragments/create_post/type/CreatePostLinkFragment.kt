package dev.gtcl.astro.ui.fragments.create_post.type

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import dev.gtcl.astro.LinkPost
import dev.gtcl.astro.R
import dev.gtcl.astro.databinding.FragmentCreatePostLinkBinding
import dev.gtcl.astro.ui.fragments.create_post.CreatePostVM

class CreatePostLinkFragment : Fragment() {

    private var binding: FragmentCreatePostLinkBinding? = null

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
        binding = FragmentCreatePostLinkBinding.inflate(inflater)
        return binding?.root
    }

    private fun initObservers() {
        model.fetchInput.observe(viewLifecycleOwner, {
            if (it == true) {
                val text = binding?.fragmentCreatePostLinkText?.text.toString()
                if (text.isEmpty()) {
                    binding?.fragmentCreatePostLinkTextInputLayout?.error =
                        getString(R.string.required)
                } else {
                    try {
                        if (!URLUtil.isValidUrl(text)) {
                            throw Exception()
                        }
                        model.setPostContent(LinkPost(text))
                    } catch (e: Exception) {
                        binding?.fragmentCreatePostLinkTextInputLayout?.error =
                            getString(R.string.invalid)
                    }
                }
                model.dataFetched()
            }
        })

        binding?.fragmentCreatePostLinkText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding?.fragmentCreatePostLinkTextInputLayout?.error = null
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