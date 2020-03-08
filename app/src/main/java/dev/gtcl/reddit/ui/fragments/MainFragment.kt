package dev.gtcl.reddit.ui.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2

import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.databinding.FragmentMainBinding
import dev.gtcl.reddit.ui.MainActivity
import dev.gtcl.reddit.ui.MainActivityViewModel

/**
 * A simple [Fragment] subclass.
 */
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    private lateinit var adapter: MainFragmentStateAdapter

    val model: MainFragmentViewModel by lazy {
        val viewModelFactory = MainFragmentViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MainFragmentViewModel::class.java)
    }

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater)

        // TODO: Listener for refresh token
        parentModel.fetchData.observe(viewLifecycleOwner, Observer {
            if(it){
                model.fetchDefaultSubreddits()
                model.fetchPopularPosts()
                model.fetchTrendingPosts()
            }
        })
        parentModel.currentUser.observe(viewLifecycleOwner, Observer {
            model.fetchDefaultSubreddits()
        })
        setViewPagerAdapter()
        return binding.root
    }

    private fun setViewPagerAdapter(){
        adapter = MainFragmentStateAdapter(this)
        adapter.setOnPostClickedListener {
            adapter.setCommentPage(it)
            binding.viewPager.setCurrentItem(1, true)
        }
        adapter.setCommentsFragmentListener(object :
            MainFragmentViewPagerListener {
            override fun enablePagerSwiping(enable: Boolean) {
                binding.viewPager.isUserInputEnabled = enable
            }

            override fun navigateToPostList() {
                binding.viewPager.setCurrentItem(0, true)
                adapter.resetCommentPage()
            }
        })
        binding.viewPager.adapter = adapter
        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.viewPager.isUserInputEnabled = (position != 0)
                if(position == 0)
                    adapter.resetCommentPage()
            }
        })

        binding.viewPager.setPageTransformer(DepthPageTransformer())

    }
}
