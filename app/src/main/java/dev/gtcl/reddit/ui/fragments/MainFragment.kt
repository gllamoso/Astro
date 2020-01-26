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
import dev.gtcl.reddit.subs.Subreddit

/**
 * A simple [Fragment] subclass.
 */
class MainFragment : Fragment() {

    private lateinit var binding: FragmentMainBinding
    val model: MainFragmentViewModel by lazy {
        val viewModelFactory =
            MainFragmentViewModelFactory(
                activity!!.application as RedditApplication
            )
        ViewModelProvider(this, viewModelFactory).get(MainFragmentViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(inflater)
        binding.viewPager.adapter =
            MainFragmentStateAdapter(this)
        binding.viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.viewPager.isUserInputEnabled = (position != 0)
            }
        })

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

        // TODO: Update
        model.getPosts(Subreddit(displayName = "funny"))
        model.getDefaultSubreddits()
        model.getTrendingPosts()
        model.getPopularPosts()
        return binding.root
    }


}
