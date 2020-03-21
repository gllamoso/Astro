package dev.gtcl.reddit.ui.fragments.account.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentUserBinding
import dev.gtcl.reddit.ui.MainActivity
import dev.gtcl.reddit.ui.MainActivityViewModel
import dev.gtcl.reddit.ui.ViewPagerActions
import dev.gtcl.reddit.ui.fragments.account.user.comments.UserCommentsFragment
import dev.gtcl.reddit.ui.fragments.account.user.downvoted.UserDownvotedFragment
import dev.gtcl.reddit.ui.fragments.account.user.hidden.UserHiddenFragment
import dev.gtcl.reddit.ui.fragments.account.user.overview.UserOverviewFragment
import dev.gtcl.reddit.ui.fragments.account.user.saved.UserSavedFragment
import dev.gtcl.reddit.ui.fragments.account.user.upvoted.UserUpvotedFragment

class UserFragment() : Fragment() {

    private lateinit var viewPagerActions: ViewPagerActions
    fun setViewPagerActions(viewPagerActions: ViewPagerActions){
        this.viewPagerActions = viewPagerActions
    }

    private lateinit var binding: FragmentUserBinding

    val model: UserFragmentViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(UserFragmentViewModel::class.java)
    }

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is UserCommentsFragment -> childFragment.setViewPagerActions(viewPagerActions)
            is UserSavedFragment -> childFragment.setViewPagerActions(viewPagerActions)
            is UserUpvotedFragment -> childFragment.setViewPagerActions(viewPagerActions)
            is UserDownvotedFragment -> childFragment.setViewPagerActions(viewPagerActions)
            is UserHiddenFragment -> childFragment.setViewPagerActions(viewPagerActions)
            is UserOverviewFragment -> childFragment.setViewPagerActions(viewPagerActions)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentUserBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model
        val user = "Midoy192" // TODO: Update
        setupViewPagerAdapter(user == parentModel.currentUser.value?.name)
        model.fetchCurrentUser()
        model.fetchListings()
        return binding.root
    }

    private fun setupViewPagerAdapter(isCurrentUser: Boolean){
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout
        val adapter = UserStateAdapter(this)
        viewPager.adapter = adapter
        if(isCurrentUser){
            TabLayoutMediator(tabLayout, viewPager){ tab, position ->
                tab.text = getText(when(position){
                    0 -> R.string.about
                    1 -> R.string.overview
                    2 -> R.string.posts
                    3 -> R.string.comments
                    4 -> R.string.saved
                    5 -> R.string.hidden
                    6 -> R.string.upvoted
                    7 -> R.string.downvoted
                    8 -> R.string.gilded
                    9 -> R.string.friends
                    10 -> R.string.blocked
                    else -> throw NoSuchElementException("No such tab in the following position: $position")
                })
            }.attach()
        } else {
            TabLayoutMediator(tabLayout, viewPager){ tab, position ->
                tab.text = getText(when(position){
                    0 -> R.string.about
                    1 -> R.string.overview
                    2 -> R.string.posts
                    3 -> R.string.comments
                    4 -> R.string.gilded
                    else -> throw NoSuchElementException("No such tab in the following position: $position")
                })
            }.attach()
        }
    }
}