package dev.gtcl.reddit.ui.fragments.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.databinding.FragmentUserBinding
import dev.gtcl.reddit.ui.activities.main.MainActivity
import dev.gtcl.reddit.ui.activities.main.MainActivityViewModel
import dev.gtcl.reddit.actions.ViewPagerActions
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.ui.fragments.ListingScrollerFragment

class AccountFragment : Fragment(), ItemClickListener {

    private lateinit var binding: FragmentUserBinding

    private var viewPagerActions: ViewPagerActions? = null
    fun setFragment(viewPagerActions: ViewPagerActions){
        this.viewPagerActions = viewPagerActions
//        model.fetchAccount(user)
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
//            is SimpleListingScrollerFragment -> childFragment.setActions(postActions = this, user = model.username)
            is ListingScrollerFragment -> childFragment.setActions(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentUserBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model
        setupViewPagerAdapter()
        binding.toolbar.setNavigationOnClickListener {
//            parentModel.openDrawer()
        }
        val user = requireArguments().getString(USER_KEY)
        model.fetchAccount(user)
        return binding.root
    }

    private fun setupViewPagerAdapter(){
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout
        val adapter =
            AccountStateAdapter(
                this,
                model.username
            )
        viewPager.adapter = adapter
        if(model.username == null){
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

    override fun itemClicked(item: Item) {
        viewPagerActions?.navigateToNewPage(item)
    }

    companion object {
        fun newInstance(user: String? = null): AccountFragment {
            val fragment = AccountFragment()
            val args = bundleOf(USER_KEY to user)
            fragment.arguments = args
            return fragment
        }
    }
}