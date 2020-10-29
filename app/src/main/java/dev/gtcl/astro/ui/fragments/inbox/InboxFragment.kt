package dev.gtcl.astro.ui.fragments.inbox

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.LeftDrawerActions
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.databinding.FragmentInboxBinding
import dev.gtcl.astro.models.reddit.listing.FrontPage
import dev.gtcl.astro.ui.LeftDrawerAdapter
import dev.gtcl.astro.ui.LeftDrawerHeader
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.view_pager.AccountPage
import dev.gtcl.astro.ui.fragments.view_pager.ListingPage
import dev.gtcl.astro.ui.fragments.view_pager.ViewPagerFragmentDirections
import dev.gtcl.astro.ui.fragments.view_pager.ViewPagerVM
import dev.gtcl.astro.url.URL

class InboxFragment : Fragment(), LeftDrawerActions {

    private var binding: FragmentInboxBinding? = null

    private val activityModel: MainActivityVM by activityViewModels()

    private val viewPagerModel: ViewPagerVM by lazy {
        ViewModelProviders.of(requireParentFragment()).get(ViewPagerVM::class.java)
    }

    private var pageSelected = 0

    override fun onResume() {
        super.onResume()
        viewPagerModel.syncViewPager()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInboxBinding.inflate(inflater)
        initViewPagerAdapter()
        initLeftDrawer()

        binding?.fragmentInboxFab?.setOnClickListener {
            ComposeDialogFragment.newInstance().show(childFragmentManager, null)
        }

        childFragmentManager.setFragmentResultListener(DRAFT_KEY, viewLifecycleOwner, { _, bundle ->
            val draft = bundle.get(DRAFT_KEY) as Draft
            SaveDraftDialogFragment.newInstance(draft)
                .show(childFragmentManager, null)
        })

        childFragmentManager.setFragmentResultListener(URL_KEY, viewLifecycleOwner, { _, bundle ->
            val url = bundle.getString(URL_KEY) ?: return@setFragmentResultListener
            viewPagerModel.linkClicked(URL(url))
        })

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.fragmentInboxViewPager?.adapter = null
        binding = null
    }

    private fun initViewPagerAdapter() {
        val adapter = InboxStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding?.fragmentInboxViewPager?.adapter = adapter
        if (binding?.fragmentInboxViewPager != null && binding?.fragmentInboxTabLayout != null) {
            TabLayoutMediator(
                (binding ?: return).fragmentInboxTabLayout,
                (binding ?: return).fragmentInboxViewPager
            ) { tab, position ->
                tab.text = getText(
                    when (position) {
                        0 -> R.string.inbox
                        1 -> R.string.unread
                        2 -> R.string.sent
                        else -> throw NoSuchElementException("No such tab in the following position: $position")
                    }
                )
            }.attach()
            binding?.fragmentInboxViewPager?.currentItem = pageSelected
            binding?.fragmentInboxViewPager?.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    pageSelected = position
                }
            })
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun initLeftDrawer() {

        val leftDrawerAdapter = LeftDrawerAdapter(requireContext(), this, LeftDrawerHeader.HOME)
        val leftDrawerLayout = binding?.fragmentInboxLeftDrawerLayout
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

        binding?.fragmentInboxToolbar?.setNavigationOnClickListener {
            binding?.fragmentInboxDrawer?.openDrawer(Gravity.LEFT)
        }
    }


//     _           __ _     _____                                             _   _
//    | |         / _| |   |  __ \                                  /\       | | (_)
//    | |     ___| |_| |_  | |  | |_ __ __ ___      _____ _ __     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \  _| __| | |  | | '__/ _` \ \ /\ / / _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___|  __/ | | |_  | |__| | | | (_| |\ V  V /  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//    |______\___|_|  \__| |_____/|_|  \__,_| \_/\_/ \___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/
//


    @SuppressLint("RtlHardcoded")
    override fun onAddAccountClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSignInFragment())
        binding?.fragmentInboxDrawer?.closeDrawer(Gravity.LEFT)
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
        binding?.fragmentInboxDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onLogoutClicked() {
        val application = (requireActivity().application as AstroApplication)
        val currentAccount = application.currentAccount
        if (currentAccount != null) {
            application.saveAccount(null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding?.fragmentInboxDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onHomeClicked() {
        findNavController().navigate(
            ViewPagerFragmentDirections.popBackStack(ListingPage(FrontPage))
        )
        binding?.fragmentInboxDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onMyAccountClicked() {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(null))
            )
            binding?.fragmentInboxDrawer?.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onInboxClicked() {
        binding?.fragmentInboxDrawer?.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onSettingsClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSettingsFragment())
        binding?.fragmentInboxDrawer?.closeDrawer(Gravity.LEFT)
    }

    companion object {
        fun newInstance() = InboxFragment()
    }
}
