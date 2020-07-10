package dev.gtcl.reddit.ui.fragments.create_post

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.*
import dev.gtcl.reddit.databinding.FragmentCreatePostBinding

class CreatePostFragment : Fragment(){

    private lateinit var binding: FragmentCreatePostBinding

    private val args: CreatePostFragmentArgs by navArgs()

    private val model: CreatePostVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(CreatePostVM::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreatePostBinding.inflate(inflater)

        initSubredditText()
        initViewPagerAdapter()
        initToolbarObservers()
        initModelObservers()

        return binding.root
    }

    private fun initSubredditText(){
        val subredditName = args.subredditName
        binding.subredditText.setText(subredditName)
    }

    private fun initViewPagerAdapter(){
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout
        val adapter = CreatePostStateAdapter(this)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager){ tab, position ->
            tab.text = getText(when(position){
                0 -> R.string.text
                1 -> R.string.image
                2 -> R.string.link
                else -> throw NoSuchElementException("No such tab in the following position: $position")
            })
        }.attach()
    }

    private fun initToolbarObservers(){
        val toolbar = binding.toolbar

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.send -> {
                    model.fetchData()
                }
            }
            true
        }
    }

    private fun initModelObservers(){
        model.postContent.observe(viewLifecycleOwner, Observer { postContent ->
            postContent?.let {
                val sub = binding.subredditText.text?.toString() ?: ""
                val title = binding.titleText.text?.toString() ?: ""
                val body = when(it){
                    is TextPost -> it.body
                    is ImagePost -> it.file.absolutePath
                    is LinkPost -> it.url
                }
                Log.d("TAE", "Sub: $sub, Title: $title, Body: $body")
            }
        })
    }
}