package dev.gtcl.reddit.ui.fragments.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.*
import dev.gtcl.reddit.databinding.FragmentUserBinding
import dev.gtcl.reddit.database.SavedAccount
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.ListingType
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.PostPage
import dev.gtcl.reddit.ui.fragments.ViewPagerVM
import dev.gtcl.reddit.ui.fragments.item_scroller.ItemScrollerFragment

class AccountFragment : Fragment(), LeftDrawerActions, NavigationActions, ViewPagerActions {

    private lateinit var binding: FragmentUserBinding

    val model: AccountFragmentVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(AccountFragmentVM::class.java)
    }

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
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

//     _           __ _     _____                                             _   _
//    | |         / _| |   |  __ \                                  /\       | | (_)
//    | |     ___| |_| |_  | |  | |_ __ __ ___      _____ _ __     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \  _| __| | |  | | '__/ _` \ \ /\ / / _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___|  __/ | | |_  | |__| | | | (_| |\ V  V /  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//    |______\___|_|  \__| |_____/|_|  \__,_| \_/\_/ \___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/

    override fun onAddAccountClicked() {
//        navigationActions?.signInNewAccount()
    }

    override fun onRemoveAccountClicked(account: SavedAccount) {
        TODO("Not yet implemented")
    }

    override fun onAccountClicked(account: SavedAccount) {
        TODO("Not yet implemented")
    }

    override fun onLogoutClicked() {
        TODO("Not yet implemented")
    }

    override fun onHomeClicked() {
        TODO("Not yet implemented")
    }

    override fun onMyAccountClicked() {
//        navigationActions?.accountSelected(null)
    }

    override fun onInboxClicked() {
//        navigationActions?.messagesSelected()
    }

    override fun onSettingsClicked() {
        TODO("Not yet implemented")
    }

//     _   _             _             _   _                            _   _
//    | \ | |           (_)           | | (_)                 /\       | | (_)
//    |  \| | __ ___   ___  __ _  __ _| |_ _  ___  _ __      /  \   ___| |_ _  ___  _ __  ___
//    | . ` |/ _` \ \ / / |/ _` |/ _` | __| |/ _ \| '_ \    / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |\  | (_| |\ V /| | (_| | (_| | |_| | (_) | | | |  / ____ \ (__| |_| | (_) | | | \__ \
//    |_| \_|\__,_| \_/ |_|\__, |\__,_|\__|_|\___/|_| |_| /_/    \_\___|\__|_|\___/|_| |_|___/
//                          __/ |
//                         |___/

    override fun listingSelected(listing: ListingType) {
//        navigationActions?.listingSelected(listing)
    }

    override fun accountSelected(user: String?) {
//        navigationActions?.accountSelected(user)
    }

    override fun messagesSelected() {
//        navigationActions?.messagesSelected()
    }

    override fun signInNewAccount() {
//        navigationActions?.signInNewAccount()
    }

    override fun launchWebview(url: String) {
//        navigationActions?.launchWebview(url)

    }

//    __      ___               _____                                     _   _
//    \ \    / (_)             |  __ \                          /\       | | (_)
//     \ \  / / _  _____      _| |__) |_ _  __ _  ___ _ __     /  \   ___| |_ _  ___  _ __  ___
//      \ \/ / | |/ _ \ \ /\ / /  ___/ _` |/ _` |/ _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//       \  /  | |  __/\ V  V /| |  | (_| | (_| |  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//        \/   |_|\___| \_/\_/ |_|   \__,_|\__, |\___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/
//                                          __/ |
//                                         |___/

    override fun enablePagerSwiping(enable: Boolean) {
//        viewPagerActions?.enablePagerSwiping(enable)
    }

    override fun navigatePreviousPage() {
//        viewPagerActions?.navigatePreviousPage()
    }

    override fun navigateToComments(post: Post, position: Int) {
//        viewPagerActions?.navigateToComments(post, position)
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