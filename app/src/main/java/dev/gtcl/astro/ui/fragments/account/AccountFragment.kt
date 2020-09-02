package dev.gtcl.astro.ui.fragments.account

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.*
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.databinding.FragmentAccountBinding
import dev.gtcl.astro.databinding.PopupAccountActionsBinding
import dev.gtcl.astro.models.reddit.listing.Account
import dev.gtcl.astro.models.reddit.listing.FrontPage
import dev.gtcl.astro.ui.LeftDrawerAdapter
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.*

class AccountFragment : Fragment(),  LeftDrawerActions {

    private lateinit var binding: FragmentAccountBinding

    val model: AccountFragmentVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(AccountFragmentVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAccountBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model
        val username = requireArguments().getString(USER_KEY)
        if(model.username == null){
            model.setUsername(username)
        }
        if(model.account.value == null){
            model.fetchAccount(username)
        }

        initViewPagerAdapter()
        initLeftDrawer()
        initOtherObservers()

        return binding.root
    }

    @SuppressLint("RtlHardcoded")
    private fun initLeftDrawer(){
        val leftDrawerAdapter = LeftDrawerAdapter(requireContext(), this, LeftDrawerHeader.HOME)
        val leftDrawerLayout = binding.fragmentAccountLeftDrawerLayout
        leftDrawerLayout.layoutLeftDrawerList.adapter = leftDrawerAdapter
        leftDrawerLayout.account = (requireActivity().application as AstroApplication).currentAccount

        activityModel.allUsers.observe(viewLifecycleOwner, {
            leftDrawerAdapter.submitUsers(it)
        })

        leftDrawerLayout.layoutLeftDrawerBanner.setOnClickListener {
            leftDrawerAdapter.toggleExpanded()
            rotateView(leftDrawerLayout.layoutLeftDrawerExpandedIndicator, leftDrawerAdapter.isExpanded)
        }

        binding.fragmentAccountToolbar.setNavigationOnClickListener {
            binding.fragmentAccountDrawer.openDrawer(Gravity.LEFT)
        }
    }

    private fun initViewPagerAdapter(){
        val viewPager = binding.fragmentAccountViewPager
        val tabLayout = binding.fragmentAccountTabLayout
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

    private fun initOtherObservers(){
        model.errorMessage.observe(viewLifecycleOwner, {
            if(it != null){
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
                binding.fragmentAccountViewPager.adapter = null
            }
        })

        binding.fragmentAccountSubscribeToggle.root.setOnClickListener {
            checkedIfLoggedInBeforeExecuting(requireContext()){
                val sub = model.account.value?.subreddit ?: return@checkedIfLoggedInBeforeExecuting
                sub.userSubscribed = sub.userSubscribed != true
                binding.invalidateAll()
                activityModel.subscribe(sub, (sub.userSubscribed == true))
            }
        }

        binding.fragmentAccountToolbar.setOnMenuItemClickListener {
            val account = model.account.value
            if(account != null){
                val anchor = getMenuItemView(binding.fragmentAccountToolbar, R.id.more_options)
                showAccountActionsPopup(anchor!!, account)
            }
            true
        }

        childFragmentManager.setFragmentResultListener(URL_KEY, viewLifecycleOwner, { _, bundle ->
            val url = bundle.getString(URL_KEY)!!
            viewPagerModel.linkClicked(url)
        })
    }

    private fun showAccountActionsPopup(anchor: View, account: Account){
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupAccountActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.account = account
            popupAccountActionsFriend.root.setOnClickListener {
                checkedIfLoggedInBeforeExecuting(requireContext()){
                    account.isFriend = !(account.isFriend ?: false)
                    if(account.isFriend == true){
                        model.addFriend(account.name)
                    } else {
                        model.unfriend(account.name)
                    }
                }
                popupWindow.dismiss()
            }
            popupAccountActionsBlock.root.setOnClickListener {
                checkedIfLoggedInBeforeExecuting(requireContext()){
                    model.blockUser(account.name)
                    findNavController().popBackStack()
                }
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        popupWindow.showAsDropdown(anchor, popupBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, popupBinding.root.measuredHeight)
    }

//     _           __ _     _____                                             _   _
//    | |         / _| |   |  __ \                                  /\       | | (_)
//    | |     ___| |_| |_  | |  | |_ __ __ ___      _____ _ __     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \  _| __| | |  | | '__/ _` \ \ /\ / / _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___|  __/ | | |_  | |__| | | | (_| |\ V  V /  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//    |______\___|_|  \__| |_____/|_|  \__,_| \_/\_/ \___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/

    @SuppressLint("RtlHardcoded")
    override fun onAddAccountClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSignInFragment())
        binding.fragmentAccountDrawer.closeDrawer(Gravity.LEFT)
    }

    override fun onRemoveAccountClicked(account: SavedAccount) {
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        if(account.id == currentAccount?.id){
            saveAccountToPreferences(requireContext(), null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        activityModel.removeAccount(account)
    }

    @SuppressLint("RtlHardcoded")
    override fun onAccountClicked(account: SavedAccount) {
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        if(account.id != currentAccount?.id){
            saveAccountToPreferences(requireContext(), account)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding.fragmentAccountDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onLogoutClicked() {
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        if(currentAccount != null){
            saveAccountToPreferences(requireContext(), null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding.fragmentAccountDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onHomeClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.popBackStack(ListingPage(FrontPage)))
        binding.fragmentAccountDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onMyAccountClicked() {
        val user = model.account.value
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        if(user?.name != currentAccount?.name){
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(null)))
        }
        binding.fragmentAccountDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onInboxClicked() {
        if ((activity?.application as AstroApplication).accessToken == null) {
            Snackbar.make(binding.fragmentAccountDrawer, R.string.must_be_logged_in, Snackbar.LENGTH_SHORT).show()
        } else {
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(InboxPage))
            binding.fragmentAccountDrawer.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onSettingsClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSettingsFragment())
        binding.fragmentAccountDrawer.closeDrawer(Gravity.LEFT)
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