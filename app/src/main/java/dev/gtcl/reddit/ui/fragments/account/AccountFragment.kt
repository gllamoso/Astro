package dev.gtcl.reddit.ui.fragments.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.LeftDrawerActions
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.databinding.FragmentUserBinding
import dev.gtcl.reddit.actions.ViewPagerActions
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Post
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.item_scroller.ItemScrollerFragment

class AccountFragment : Fragment(), ItemClickListener, PostActions, LeftDrawerActions {

    private lateinit var binding: FragmentUserBinding

    private var viewPagerActions: ViewPagerActions? = null
    private var parentPostActions: PostActions? = null

    fun setActions(viewPagerActions: ViewPagerActions, postActions: PostActions){
        this.viewPagerActions = viewPagerActions
        parentPostActions = postActions
    }

    val model: AccountFragmentVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(AccountFragmentVM::class.java)
    }

    private val parentViewModel: MainActivityVM by activityViewModels()

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is ItemScrollerFragment -> childFragment.setActions(this, postActions = this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentUserBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model
        binding.toolbar.setNavigationOnClickListener {
//            parentModel.openDrawer()
        }
        val username = requireArguments().getString(USER_KEY)
        model.setUsername(username)
        model.fetchAccount(username)
        setupViewPagerAdapter()
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

//     _____          _                  _   _
//    |  __ \        | |       /\       | | (_)
//    | |__) |__  ___| |_     /  \   ___| |_ _  ___  _ __  ___
//    |  ___/ _ \/ __| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | (_) \__ \ |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_|   \___/|___/\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun vote(post: Post, vote: Vote) {
        parentPostActions?.vote(post, vote)
    }

    override fun share(post: Post) {
        parentPostActions?.share(post)
    }

    override fun viewProfile(post: Post) {
        parentPostActions?.viewProfile(post)
    }

    override fun save(post: Post) {
        parentPostActions?.save(post)
    }

    override fun hide(post: Post) {
        parentPostActions?.hide(post)
    }

    override fun report(post: Post) {
        parentPostActions?.report(post)
    }

    override fun thumbnailClicked(post: Post) {
        parentPostActions?.thumbnailClicked(post)
    }

//     _           __ _     _____                                             _   _
//    | |         / _| |   |  __ \                                  /\       | | (_)
//    | |     ___| |_| |_  | |  | |_ __ __ ___      _____ _ __     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \  _| __| | |  | | '__/ _` \ \ /\ / / _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___|  __/ | | |_  | |__| | | | (_| |\ V  V /  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//    |______\___|_|  \__| |_____/|_|  \__,_| \_/\_/ \___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/

    override fun onAddAccountClicked() {
        TODO("Not yet implemented")
    }

    override fun onRemoveAccountClicked(user: String) {
        TODO("Not yet implemented")
    }

    override fun onAccountClicked(account: Account) {
        TODO("Not yet implemented")
    }

    override fun onLogoutClicked() {
        TODO("Not yet implemented")
    }

    override fun onHomeClicked() {
        TODO("Not yet implemented")
    }

    override fun onMyAccountClicked() {
        TODO("Not yet implemented")
    }

    override fun onInboxClicked() {
        TODO("Not yet implemented")
    }

    override fun onSettingsClicked() {
        TODO("Not yet implemented")
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