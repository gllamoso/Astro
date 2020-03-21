package dev.gtcl.reddit.ui.fragments.posts


import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentMainBinding
import dev.gtcl.reddit.network.Comment
import dev.gtcl.reddit.network.Post
import dev.gtcl.reddit.ui.MainActivity
import dev.gtcl.reddit.ui.MainActivityViewModel
import dev.gtcl.reddit.ui.ViewPagerActions
import dev.gtcl.reddit.ui.fragments.*
import dev.gtcl.reddit.ui.fragments.comments.CommentsFragment
import dev.gtcl.reddit.ui.fragments.posts.listing.ListingFragment

/**
 * A simple [Fragment] subclass.
 */
class ListingViewPagerFragment : Fragment(), ViewPagerActions {

    private lateinit var binding: FragmentMainBinding
    private lateinit var pageAdapter: ViewPagerAdapter

    val model: ListingViewPagerViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ListingViewPagerViewModel::class.java)
    }

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is ListingFragment -> childFragment.setViewPagerActions(this)
            is CommentsFragment -> childFragment.setViewPagerActions(this)
        }
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
        pageAdapter = ViewPagerAdapter(this, StartingViewPagerFragments.LISTING, this)

        binding.viewPager.apply {
            adapter = pageAdapter
            registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                var previousPage = 0
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if(state == ViewPager2.SCROLL_STATE_IDLE){
                        if(previousPage > currentItem)
                            pageAdapter.popFragment()
                    }
                    previousPage = currentItem

                }
            })
            setPageTransformer(SlidePageTransformer())
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
    }

    // View Pager Actions
    override fun enablePagerSwiping(enable: Boolean) {
        binding.viewPager.isUserInputEnabled = enable
    }

    override fun navigatePrevious() {
        val currentPage = binding.viewPager.currentItem
        binding.viewPager.setCurrentItem(currentPage - 1, true)
    }

    override fun viewComments(post: Post) {
        pageAdapter.addCommentsPage(post)
        navigateNext()
    }

    override fun viewComments(comment: Comment) {
//        navigateNext()
    }

    override fun viewThumbnail(url: String) {
//        navigateNext()
    }

    private fun navigateNext() {
        val currentPage = binding.viewPager.currentItem
        binding.viewPager.setCurrentItem(currentPage + 1, true)
    }
}
