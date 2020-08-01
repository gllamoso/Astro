package dev.gtcl.reddit.ui.fragments.inbox

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.LeftDrawerActions
import dev.gtcl.reddit.actions.MessageActions
import dev.gtcl.reddit.database.SavedAccount
import dev.gtcl.reddit.databinding.FragmentInboxBinding
import dev.gtcl.reddit.models.reddit.listing.Message
import dev.gtcl.reddit.ui.fragments.AccountPage

class InboxFragment: Fragment(), MessageActions, LeftDrawerActions{

    private lateinit var binding: FragmentInboxBinding

//    private val parentModel: MainActivityViewModel by lazy {
//        (activity as MainActivity).model
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInboxBinding.inflate(inflater)
        setViewPagerAdapter()
        setLeftDrawer()
        return binding.root
    }

    private fun setViewPagerAdapter(){
        binding.viewPager.adapter = InboxStateAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getText(when(position){
                0 -> R.string.inbox
                1 -> R.string.unread
                2 -> R.string.sent
                else -> throw NoSuchElementException("No such tab in the following position: $position")
            })
        }.attach()
    }

    private fun setLeftDrawer(){
//        val header = LayoutNavHeaderBinding.inflate(inflater)
//        binding.expandableListView.addHeaderView(header.root)

//        binding.expandableListView.setAdapter(adapter)

//        parentModel.allUsers.observe(viewLifecycleOwner, Observer {
//            adapter.setUsers(it.asAccountDomainModel())
//        })
//
//        parentModel.currentAccount.observe(viewLifecycleOwner, Observer {
//            header.account = it
//        })

//        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
//        binding.toolbar.setNavigationOnClickListener {
//            binding.drawerLayout.openDrawer(Gravity.LEFT)
//        }
//        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener{
//            override fun onDrawerStateChanged(newState: Int) {}
//            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
//            override fun onDrawerClosed(drawerView: View) {
//                binding.expandableListView.collapseGroup(0)
//            }
//
//            override fun onDrawerOpened(drawerView: View) {
//                adapter.notifyDataSetInvalidated()
//            }
//        })
    }

//     __  __                                               _   _
//    |  \/  |                                    /\       | | (_)
//    | \  / | ___  ___ ___  __ _  __ _  ___     /  \   ___| |_ _  ___  _ __  ___
//    | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | |  __/\__ \__ \ (_| | (_| |  __/  / ____ \ (__| |_| | (_) | | | \__ \
//    |_|  |_|\___||___/___/\__,_|\__, |\___| /_/    \_\___|\__|_|\___/|_| |_|___/
//                                 __/ |
//                                |___/

    override fun reply(message: Message) {
        TODO("Not yet implemented")
    }

    override fun mark(message: Message) {
        TODO("Not yet implemented")
    }

    override fun delete(message: Message) {
        TODO("Not yet implemented")
    }

    override fun viewProfile(user: String) {
        TODO("Not yet implemented")
    }

    override fun block(user: String) {
        TODO("Not yet implemented")
    }

//     _           __ _     _____                                             _   _
//    | |         / _| |   |  __ \                                  /\       | | (_)
//    | |     ___| |_| |_  | |  | |_ __ __ ___      _____ _ __     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \  _| __| | |  | | '__/ _` \ \ /\ / / _ \ '__|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___|  __/ | | |_  | |__| | | | (_| |\ V  V /  __/ |     / ____ \ (__| |_| | (_) | | | \__ \
//    |______\___|_|  \__| |_____/|_|  \__,_| \_/\_/ \___|_|    /_/    \_\___|\__|_|\___/|_| |_|___/
//


    override fun onAddAccountClicked() {
//        parentModel.startSignInActivity()
    }

    override fun onRemoveAccountClicked(account: SavedAccount) {
//        parentModel.deleteUserFromDatabase(user)
    }

    @SuppressLint("RtlHardcoded")
    override fun onAccountClicked(account: SavedAccount) {
//        parentModel.setCurrentUser(account, true)
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onLogoutClicked() {
//        parentModel.setCurrentUser(null, true)
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onHomeClicked() {
//        findNavController().popBackStack(R.id.home_fragment, false)
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onMyAccountClicked() {
        findNavController().navigate(InboxFragmentDirections.actionInboxFragmentToViewPagerFragment(AccountPage(null)))
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onInboxClicked() {
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }

    @SuppressLint("RtlHardcoded")
    override fun onSettingsClicked() {
        Toast.makeText(context, "Settings", Toast.LENGTH_LONG).show()
        binding.drawerLayout.closeDrawer(Gravity.LEFT)
    }
}
