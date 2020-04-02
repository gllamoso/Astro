package dev.gtcl.reddit.ui.fragments.account

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
import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.databinding.FragmentUserBinding
import dev.gtcl.reddit.listings.Post
import dev.gtcl.reddit.ui.activities.MainActivity
import dev.gtcl.reddit.ui.activities.MainActivityViewModel
import dev.gtcl.reddit.ui.PostActions
import dev.gtcl.reddit.ui.ViewPagerActions
import dev.gtcl.reddit.ui.fragments.account.pages.UserAboutFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserCommentsFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserDownvotedFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserHiddenFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserOverviewFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserPostsFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserSavedFragment
import dev.gtcl.reddit.ui.fragments.account.pages.UserUpvotedFragment

class AccountFragment : Fragment(), PostActions {

    private lateinit var binding: FragmentUserBinding

    private lateinit var viewPagerActions: ViewPagerActions
    fun setFragment(viewPagerActions: ViewPagerActions, user: String?){
        this.viewPagerActions = viewPagerActions
        model.fetchAccount(user)
    }

    val model: AccountFragmentViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(AccountFragmentViewModel::class.java)
    }

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is UserAboutFragment -> childFragment.setUser(model.username)
            is UserCommentsFragment -> childFragment.setFragment(this, model.username)
            is UserPostsFragment -> childFragment.setFragment(this, model.username)
            is UserSavedFragment -> childFragment.setFragment(this)
            is UserUpvotedFragment -> childFragment.setFragment(this)
            is UserDownvotedFragment -> childFragment.setFragment(this)
            is UserHiddenFragment -> childFragment.setFragment(this)
            is UserOverviewFragment -> childFragment.setFragment(this, model.username)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentUserBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model
        setupViewPagerAdapter(model.username == null)
        model.fetchListings()
        return binding.root
    }

    private fun setupViewPagerAdapter(isCurrentUser: Boolean){
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout
        val adapter =
            AccountStateAdapter(
                this,
                isCurrentUser
            )
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

    // Post Actions
    override fun vote(post: Post, vote: Vote) {
        TODO("Not yet implemented")
    }

    override fun share(post: Post) {
        TODO("Not yet implemented")
    }

    override fun award(post: Post) {
        TODO("Not yet implemented")
    }

    override fun save(post: Post) {
        TODO("Not yet implemented")
    }

    override fun hide(post: Post) {
        TODO("Not yet implemented")
    }

    override fun report(post: Post) {
        TODO("Not yet implemented")
    }

    override fun postClicked(post: Post) {
        viewPagerActions.viewComments(post)
    }

    override fun thumbnailClicked(post: Post) {
        TODO("Not yet implemented")
    }
}