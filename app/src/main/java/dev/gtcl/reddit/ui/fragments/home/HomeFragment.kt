package dev.gtcl.reddit.ui.fragments.home


import android.os.Bundle
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
import dev.gtcl.reddit.databinding.FragmentViewPagerBinding
import dev.gtcl.reddit.models.reddit.Comment
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.ui.activities.main.MainActivity
import dev.gtcl.reddit.ui.activities.main.MainActivityViewModel
import dev.gtcl.reddit.actions.ViewPagerActions
import dev.gtcl.reddit.ui.fragments.*
import dev.gtcl.reddit.ui.fragments.comments.CommentsFragment
import dev.gtcl.reddit.ui.fragments.home.listing.ListingFragment

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment(), ViewPagerActions {

    private lateinit var binding: FragmentViewPagerBinding
    private lateinit var pageAdapter: ViewPagerAdapter

    val model: HomeViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(HomeViewModel::class.java)
    }

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is ListingFragment -> childFragment.setViewPagerActions(this)
            is CommentsFragment -> childFragment.setViewPagerActions(this) // TODO: Use Implement CommentActions
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentViewPagerBinding.inflate(inflater)

        // TODO: Listener for refresh token
        parentModel.fetchData.observe(viewLifecycleOwner, Observer {
            if(it){
//                model.fetchDefaultSubreddits()
            }
        })
        parentModel.currentAccount.observe(viewLifecycleOwner, Observer {
//            model.fetchDefaultSubreddits()
        })
        setViewPagerAdapter()
        return binding.root
    }

    private fun setViewPagerAdapter(){
        pageAdapter = ViewPagerAdapter(this, StartingViewPagerFragments.LISTING)

        binding.viewPager.apply {
            adapter = pageAdapter
            registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if(state == ViewPager2.SCROLL_STATE_IDLE){
                        pageAdapter.popFragments(currentItem)
                        parentModel.allowDrawerSwipe(currentItem == 0)
                    }
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
        parentModel.addReadPost(post.asReadListing)
        navigateNext()
    }

    override fun viewComments(comment: Comment) {
//        navigateNext()
    }

    private fun navigateNext() {
        val currentPage = binding.viewPager.currentItem
        binding.viewPager.setCurrentItem(currentPage + 1, true)
    }
}
