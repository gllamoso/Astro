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
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.LeftDrawerActions
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.databinding.FragmentAccountBinding
import dev.gtcl.astro.databinding.PopupAccountActionsBinding
import dev.gtcl.astro.models.reddit.listing.Account
import dev.gtcl.astro.models.reddit.listing.FrontPage
import dev.gtcl.astro.ui.LeftDrawerAdapter
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.inbox.ComposeDialogFragment
import dev.gtcl.astro.ui.fragments.inbox.Draft
import dev.gtcl.astro.ui.fragments.inbox.SaveDraftDialogFragment
import dev.gtcl.astro.ui.fragments.view_pager.*

class AccountFragment : Fragment(), LeftDrawerActions {

    private var binding: FragmentAccountBinding? = null

    val model: AccountFragmentVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(AccountFragmentVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
    }

    override fun onResume() {
        super.onResume()
        viewPagerModel.syncViewPager()
        binding?.fragmentAccountViewPager?.currentItem = model.selectedPage
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountBinding.inflate(inflater)
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.model = model
        val username = requireArguments().getString(USER_KEY)
        if (model.username == null) {
            model.setUsername(username)
        }
        if (model.account.value == null) {
            model.fetchAccount(username)
        }

        initViewPagerAdapter()
        initLeftDrawer()
        initOtherObservers()

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Glide.get(requireContext()).clearMemory()
        binding?.fragmentAccountViewPager?.adapter = null
        binding = null
    }

    @SuppressLint("RtlHardcoded")
    private fun initLeftDrawer() {
        val leftDrawerAdapter = LeftDrawerAdapter(requireContext(), this, LeftDrawerHeader.HOME)
        val leftDrawerLayout = binding?.fragmentAccountLeftDrawerLayout
        leftDrawerLayout?.layoutLeftDrawerList?.adapter = leftDrawerAdapter
        leftDrawerLayout?.account =
            (requireActivity().application as AstroApplication).currentAccount

        activityModel.allUsers.observe(viewLifecycleOwner, {
            leftDrawerAdapter.submitUsers(it)
        })

        leftDrawerLayout?.layoutLeftDrawerBanner?.setOnClickListener {
            leftDrawerAdapter.toggleExpanded()
            rotateView(
                leftDrawerLayout.layoutLeftDrawerExpandedIndicator,
                leftDrawerAdapter.isExpanded
            )
        }

        binding?.fragmentAccountToolbar?.setNavigationOnClickListener {
            binding?.fragmentAccountDrawer?.openDrawer(Gravity.LEFT)
        }
    }

    private fun initViewPagerAdapter() {
        val viewPager = binding?.fragmentAccountViewPager
        val tabLayout = binding?.fragmentAccountTabLayout
        val fragmentAdapter =
            AccountFragmentAdapter(
                childFragmentManager,
                viewLifecycleOwner.lifecycle,
                model.username
            )

        viewPager?.apply {
            this.adapter = fragmentAdapter
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    model.selectedPage = position
                }
            })
        }
        if (model.username == null) {
            if (tabLayout != null && viewPager != null) {
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = getText(
                        when (position) {
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
                        }
                    )
                }.attach()
            }
        } else {
            if (tabLayout != null && viewPager != null) {
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = getText(
                        when (position) {
                            0 -> R.string.about
                            1 -> R.string.overview
                            2 -> R.string.posts
                            3 -> R.string.comments
                            4 -> R.string.gilded
                            else -> throw NoSuchElementException("No such tab in the following position: $position")
                        }
                    )
                }.attach()
            }
        }
    }

    private fun initOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                binding?.let {
                    Snackbar.make(it.root, errorMessage, Snackbar.LENGTH_LONG).show()
                    model.errorMessageObserved()
                    binding?.fragmentAccountViewPager?.adapter = null
                }
            }
        })

        binding?.fragmentAccountSubscribeToggle?.root?.setOnClickListener {
            checkIfLoggedInBeforeExecuting(requireContext()) {
                val sub = model.account.value?.subreddit ?: return@checkIfLoggedInBeforeExecuting
                sub.userSubscribed = sub.userSubscribed != true
                binding?.fragmentAccountSubscribeToggle?.apply {
                    isSubscribed = sub.userSubscribed
                    executePendingBindings()
                }
                activityModel.subscribe(sub, (sub.userSubscribed == true))
            }
        }

        binding?.fragmentAccountToolbar?.setOnMenuItemClickListener {
            val account = model.account.value
            if (account != null) {
                val anchor = getMenuItemView(binding?.fragmentAccountToolbar, R.id.more_options)
                showAccountActionsPopup(anchor!!, account)
            }
            true
        }

        childFragmentManager.setFragmentResultListener(DRAFT_KEY, viewLifecycleOwner, { _, bundle ->
            val to = bundle.getString(TO_KEY)
            val subject = bundle.getString(SUBJECT_KEY)
            val message = bundle.getString(MESSAGE_KEY)
            val draft = Draft(to, subject, message)
            SaveDraftDialogFragment.newInstance(draft)
                .show(childFragmentManager, null)
        })
    }

    private fun showAccountActionsPopup(anchor: View, account: Account) {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupAccountActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.account = account
            popupAccountActionsFriend.root.setOnClickListener {
                checkIfLoggedInBeforeExecuting(requireContext()) {
                    account.isFriend = !(account.isFriend ?: false)
                    if (account.isFriend == true) {
                        model.addFriend(account.name)
                    } else {
                        model.unfriend(account.name)
                    }
                }
                popupWindow.dismiss()
            }
            popupAccountActionsBlock.root.setOnClickListener {
                checkIfLoggedInBeforeExecuting(requireContext()) {
                    model.blockUser(account.name)
                    findNavController().popBackStack()
                }
                popupWindow.dismiss()
            }
            popupAccountActionsSubscribe.root.setOnClickListener {
                checkIfLoggedInBeforeExecuting(requireContext()) {
                    val sub = model.account.value?.subreddit ?: return@checkIfLoggedInBeforeExecuting
                    sub.userSubscribed = sub.userSubscribed != true
                    binding?.fragmentAccountSubscribeToggle?.apply {
                        isSubscribed = sub.userSubscribed
                        executePendingBindings()
                    }
                    activityModel.subscribe(sub, (sub.userSubscribed == true))
                }
                popupWindow.dismiss()
            }
            popupAccountActionsMessage.root.setOnClickListener {
                checkIfLoggedInBeforeExecuting(requireContext()){
                    ComposeDialogFragment.newInstance(model.account.value?.name)
                        .show(childFragmentManager, null)
                }
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        popupWindow.showAsDropdown(
            anchor,
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupBinding.root.measuredHeight
        )
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
        binding?.fragmentAccountDrawer?.closeDrawer(Gravity.LEFT)
    }

    override fun onRemoveAccountClicked(account: SavedAccount) {
        val application = (requireActivity().application as AstroApplication)
        val currentAccount = application.currentAccount
        if (account.id == currentAccount?.id) {
            application.saveAccount(null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        activityModel.removeAccount(account)
    }

    @SuppressLint("RtlHardcoded")
    override fun onAccountClicked(account: SavedAccount) {
        val application = (requireActivity().application as AstroApplication)
        val currentAccount = application.currentAccount
        if (account.id != currentAccount?.id) {
            application.saveAccount(account)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding?.fragmentAccountDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onLogoutClicked() {
        val application = (requireActivity().application as AstroApplication)
        val currentAccount = application.currentAccount
        if (currentAccount != null) {
            application.saveAccount(null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding?.fragmentAccountDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onHomeClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.popBackStack(ListingPage(FrontPage)))
        binding?.fragmentAccountDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onMyAccountClicked() {
        checkIfLoggedInBeforeExecuting(requireContext()){
            val user = model.account.value
            val currentAccount = (requireActivity().application as AstroApplication).currentAccount
            if (user?.name != currentAccount?.name) {
                findNavController().navigate(
                    ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                        AccountPage(null)
                    )
                )
            }
            binding?.fragmentAccountDrawer?.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onInboxClicked() {
        checkIfLoggedInBeforeExecuting(requireContext()){
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                    InboxPage
                )
            )
            binding?.fragmentAccountDrawer?.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onSettingsClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSettingsFragment())
        binding?.fragmentAccountDrawer?.closeDrawer(Gravity.LEFT)
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