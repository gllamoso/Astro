package dev.gtcl.reddit.ui.fragments.create_post.type

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import dev.gtcl.reddit.TextPost
import dev.gtcl.reddit.databinding.FragmentCreatePostTextBinding
import dev.gtcl.reddit.ui.fragments.create_post.CreatePostVM

class TextFragment: Fragment() {

    private lateinit var binding: FragmentCreatePostTextBinding

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCreatePostTextBinding.inflate(inflater)
        binding.lifecycleOwner = this
        initObservers()
        return binding.root
    }

    private fun initObservers(){
        model.fetchData.observe(viewLifecycleOwner, Observer {
            if(it == true){
                model.setPostContent(TextPost(binding.text.text?.toString() ?: ""))
                model.dataFetched()
            }
        })
    }

    private fun removeObservers(){
        model.fetchData.removeObservers(viewLifecycleOwner)
    }
}