package dev.gtcl.reddit.ui.fragments.create_post.type

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import dev.gtcl.reddit.LinkPost
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.FragmentCreatePostLinkBinding
import dev.gtcl.reddit.ui.fragments.create_post.CreatePostVM
import java.net.URL

class LinkFragment: Fragment() {

    private lateinit var binding: FragmentCreatePostLinkBinding

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
        return binding.root
    }

    private fun initObservers(){
        model.fetchInput.observe(viewLifecycleOwner, Observer {
            if(it == true){
                val text = binding.urlText.text.toString()
                if(text.isNullOrEmpty()){
                    binding.textInputLayout.error = getString(R.string.required)
                } else {
                    try{
                        if(!URLUtil.isValidUrl(text)){
                            throw Exception()
                        }
                        val url = URL(binding.urlText.text.toString())
                        model.setPostContent(LinkPost(url))
                    } catch(e: Exception){
                        binding.textInputLayout.error = getString(R.string.invalid)
                    }
                }
                model.dataFetched()
            }
        })

        binding.urlText.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.textInputLayout.error = null
            }
        })
    }

    private fun removeObservers(){
        model.fetchInput.removeObservers(viewLifecycleOwner)
    }
}