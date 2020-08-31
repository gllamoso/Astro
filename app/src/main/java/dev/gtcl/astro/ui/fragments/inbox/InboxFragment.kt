package dev.gtcl.astro.ui.fragments.inbox

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.LeftDrawerActions
import dev.gtcl.astro.database.SavedAccount
import dev.gtcl.astro.databinding.FragmentInboxBinding
import dev.gtcl.astro.models.reddit.listing.FrontPage
import dev.gtcl.astro.ui.LeftDrawerAdapter
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.AccountPage
import dev.gtcl.astro.ui.fragments.ListingPage
import dev.gtcl.astro.ui.fragments.ViewPagerFragmentDirections

class InboxFragment: Fragment(), LeftDrawerActions{

    private lateinit var binding: FragmentInboxBinding

    private val activityModel: MainActivityVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInboxBinding.inflate(inflater)
        initViewPagerAdapter()
        initLeftDrawer()

        binding.fragmentInboxFab.setOnClickListener {
            ComposeDialogFragment.newInstance().show(childFragmentManager, null)
        }

        childFragmentManager.setFragmentResultListener(DRAFT_KEY, viewLifecycleOwner, { _, bundle ->
            AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.save_draft_question))
                .setPositiveButton(R.string.save){ _, _ ->
                    saveDraft(bundle)
                }
                .setNegativeButton(R.string.discard){ _, _ ->
                    clearSharedPreferenceDraft()
                }
                .show()
        })

        return binding.root
    }

    private fun initViewPagerAdapter(){
        binding.fragmentInboxViewPager.adapter = InboxStateAdapter(this)
        TabLayoutMediator(binding.fragmentInboxTabLayout, binding.fragmentInboxViewPager) { tab, position ->
            tab.text = getText(when(position){
                0 -> R.string.inbox
                1 -> R.string.unread
                2 -> R.string.sent
                else -> throw NoSuchElementException("No such tab in the following position: $position")
            })
        }.attach()
    }

    @SuppressLint("RtlHardcoded")
    private fun initLeftDrawer(){

        val leftDrawerAdapter = LeftDrawerAdapter(requireContext(), this, LeftDrawerHeader.HOME)
        val leftDrawerLayout = binding.fragmentInboxLeftDrawerLayout
        leftDrawerLayout.layoutLeftDrawerList.adapter = leftDrawerAdapter
        leftDrawerLayout.account = (requireActivity().application as AstroApplication).currentAccount

        activityModel.allUsers.observe(viewLifecycleOwner, {
            leftDrawerAdapter.submitUsers(it)
        })

        leftDrawerLayout.layoutLeftDrawerBanner.setOnClickListener {
            leftDrawerAdapter.toggleExpanded()
            rotateView(leftDrawerLayout.layoutLeftDrawerExpandedIndicator, leftDrawerAdapter.isExpanded)
        }

        binding.fragmentInboxToolbar.setNavigationOnClickListener {
            binding.fragmentInboxDrawer.openDrawer(Gravity.LEFT)
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
        binding.fragmentInboxDrawer.closeDrawer(Gravity.LEFT)
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
        binding.fragmentInboxDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onLogoutClicked() {
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        if(currentAccount != null){
            saveAccountToPreferences(requireContext(), null)
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSplashFragment())
        }
        binding.fragmentInboxDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onHomeClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.popBackStack(ListingPage(FrontPage)))
        binding.fragmentInboxDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onMyAccountClicked() {
        if ((activity?.application as AstroApplication).accessToken == null) {
            Snackbar.make(binding.fragmentInboxDrawer, R.string.please_login, Snackbar.LENGTH_SHORT).show()
        } else {
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(null)))
            binding.fragmentInboxDrawer.closeDrawer(Gravity.LEFT)
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onInboxClicked() {
        binding.fragmentInboxDrawer.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onSettingsClicked() {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSettingsFragment())
        binding.fragmentInboxDrawer.closeDrawer(Gravity.LEFT)
    }

    private fun clearSharedPreferenceDraft(){
        val sharedPrefs = requireContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            remove(TO_KEY)
            remove(SUBJECT_KEY)
            remove(MESSAGE_KEY)
            commit()
        }
    }

    private fun saveDraft(bundle: Bundle){
        val to = bundle.getString(TO_KEY)
        val subject = bundle.getString(SUBJECT_KEY)
        val message = bundle.getString(MESSAGE_KEY)
        val sharedPrefs = requireContext().getSharedPreferences(PREFERENCES_KEY, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString(TO_KEY, to)
            putString(SUBJECT_KEY, subject)
            putString(MESSAGE_KEY, message)
            commit()
        }
    }

    companion object{
        fun newInstance() = InboxFragment()
    }
}
