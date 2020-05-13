package dev.gtcl.reddit.ui.fragments.inbox

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.actions.LeftDrawerActions
import dev.gtcl.reddit.actions.MessageActions
import dev.gtcl.reddit.database.asAccountDomainModel
import dev.gtcl.reddit.databinding.FragmentInboxBinding
import dev.gtcl.reddit.databinding.LayoutNavHeaderBinding
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.models.reddit.Message
import dev.gtcl.reddit.ui.activities.main.MainActivity
import dev.gtcl.reddit.ui.activities.main.MainActivityViewModel
import dev.gtcl.reddit.ui.activities.main.MainDrawerAdapter
import dev.gtcl.reddit.ui.fragments.SimpleListingScrollerFragment

class MessagesFragment: Fragment(), MessageActions{

    private lateinit var binding: FragmentInboxBinding

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is SimpleListingScrollerFragment -> childFragment.setActions(messageActions = this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInboxBinding.inflate(inflater)
        setAdapter()
        setLeftDrawer(inflater)
        return binding.root
    }

    private fun setAdapter(){
        binding.viewPager.adapter = MessagesStateAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getText(when(position){
                0 -> R.string.inbox
                1 -> R.string.unread
                2 -> R.string.sent
                else -> throw NoSuchElementException("No such tab in the following position: $position")
            })
        }.attach()
    }

    private fun setLeftDrawer(inflater: LayoutInflater){
        val header = LayoutNavHeaderBinding.inflate(inflater)
        binding.expandableListView.addHeaderView(header.root)
        val adapter = MainDrawerAdapter(requireContext(),
            object :
                LeftDrawerActions {
                override fun onAddAccountClicked() {
                    parentModel.startSignInActivity()
                }

                override fun onRemoveAccountClicked(username: String) {
                    parentModel.deleteUserFromDatabase(username)
                }

                override fun onAccountClicked(account: Account) {
                    parentModel.setCurrentUser(account, true)
                    binding.drawerLayout.closeDrawer(Gravity.LEFT)
                }

                override fun onLogoutClicked() {
                    parentModel.setCurrentUser(null, true)
                    binding.drawerLayout.closeDrawer(Gravity.LEFT)
                }

                override fun onHomeClicked() {
                    findNavController().popBackStack(R.id.home_fragment, false)
                    binding.drawerLayout.closeDrawer(Gravity.LEFT)
                }

                override fun onMyAccountClicked() {
                    findNavController().navigate(R.id.account_fragment)
                    binding.drawerLayout.closeDrawer(Gravity.LEFT)
                }

                override fun onInboxClicked() {
                    if((activity?.application as RedditApplication).accessToken == null){
                        Snackbar.make(binding.drawerLayout, R.string.please_login_error, Snackbar.LENGTH_SHORT).show()
                    } else {
                        findNavController().navigate(R.id.messages_fragment)
                    }
                    binding.drawerLayout.closeDrawer(Gravity.LEFT)
                }

                override fun onSettingsClicked() {
                    Toast.makeText(context, "Settings", Toast.LENGTH_LONG).show()
                    binding.drawerLayout.closeDrawer(Gravity.LEFT)
                }

            })

        binding.expandableListView.setAdapter(adapter)

        parentModel.allUsers.observe(viewLifecycleOwner, Observer {
            adapter.setUsers(it.asAccountDomainModel())
        })

        parentModel.currentAccount.observe(viewLifecycleOwner, Observer {
            header.account = it
        })

        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.LEFT)
        }
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener{
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerClosed(drawerView: View) {
                binding.expandableListView.collapseGroup(0)
            }

            override fun onDrawerOpened(drawerView: View) {
                adapter.notifyDataSetInvalidated()
            }
        })
    }

    // Message Actions

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
}
