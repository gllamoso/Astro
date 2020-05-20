package dev.gtcl.reddit.ui.fragments.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import dev.gtcl.reddit.databinding.FragmentViewPagerBinding
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.actions.ViewPagerActions
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.ui.fragments.PageAdapter
import dev.gtcl.reddit.ui.fragments.SlidePageTransformer
import dev.gtcl.reddit.ui.fragments.comments.CommentsFragment

class AccountViewPagerFragment: Fragment(),
    ViewPagerActions {

    private lateinit var binding: FragmentViewPagerBinding
    private val pageAdapter by lazy {
        val adapter = PageAdapter(this)
        adapter.addAccountPage(null)
        adapter
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is AccountFragment -> childFragment.setFragment(this)
            is CommentsFragment -> childFragment.setViewPagerActions(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentViewPagerBinding.inflate(inflater)
        setViewPagerAdapter()
        return binding.root
    }

    private fun setViewPagerAdapter(){
        binding.viewPager.apply {
            adapter =  pageAdapter
            setPageTransformer(SlidePageTransformer())
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
                override fun onPageScrollStateChanged(state: Int) {
                    if(state == ViewPager2.SCROLL_STATE_IDLE){
                        pageAdapter.popFragmentsGreaterThanPosition(currentItem)
                    }
                    binding.viewPager.isUserInputEnabled = currentItem != 0
                }
            })
        }
    }

    // View Pager Actions
    override fun enablePagerSwiping(enable: Boolean) {
        binding.viewPager.isUserInputEnabled = enable
    }

    override fun navigatePreviousPage() {
        val currentPage = binding.viewPager.currentItem
        binding.viewPager.setCurrentItem(currentPage - 1, true)
    }

    override fun navigateToNewPage(item: Item) {
        when(item){
            is Post -> {
                pageAdapter.addPostPage(item)
                navigateToNextPage()
            }
        }
    }

    private fun navigateToNextPage(){
        binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
    }
}