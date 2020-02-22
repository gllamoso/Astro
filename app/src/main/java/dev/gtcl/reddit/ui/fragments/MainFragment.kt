package dev.gtcl.reddit.ui.fragments


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2

import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.databinding.FragmentMainBinding
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.ui.MainActivity
import dev.gtcl.reddit.ui.MainActivityViewModel

/**
 * A simple [Fragment] subclass.
 */
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }
    val model: MainFragmentViewModel by lazy {
        val viewModelFactory = MainFragmentViewModelFactory(activity!!.application as RedditApplication, parentModel.fetchAccessTokenIfNecessary)
        ViewModelProvider(this, viewModelFactory).get(MainFragmentViewModel::class.java)
    }

    val mediaController: MediaController by lazy { MediaController(context!!) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater)

        setViewPagerAdapter()

        // TODO: Update
        model.fetchPosts(Subreddit(displayName = "funny"))
        model.fetchDefaultSubreddits()
        model.fetchTrendingPosts()
        model.fetchPopularPosts()
        return binding.root
    }

    private fun setViewPagerAdapter(){
        binding.viewPager.adapter = MainFragmentStateAdapter(this)
        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if(state == ViewPager2.SCROLL_STATE_DRAGGING)
                    mediaController.hide()
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.viewPager.isUserInputEnabled = (position != 0)
                if(position == 0) model.postGenerated(false)
            }
        })

        binding.viewPager.setPageTransformer(DepthPageTransformer())

        model.currentPage.observe(viewLifecycleOwner, Observer {
            it?.let {
                binding.viewPager.setCurrentItem(it, true)
                model.scrollToPage(null)
            }
        })

        model.scrollable.observe(viewLifecycleOwner, Observer {
            it?.let{
                binding.viewPager.isUserInputEnabled = it
                model.setScrollable(null)
            }
        })
    }
}
