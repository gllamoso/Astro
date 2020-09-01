package dev.gtcl.astro.ui.fragments.account.pages.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.actions.UserActions
import dev.gtcl.astro.databinding.FragmentItemScrollerBinding
import dev.gtcl.astro.models.reddit.User
import dev.gtcl.astro.models.reddit.UserType
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.ui.fragments.AccountPage
import dev.gtcl.astro.ui.fragments.ViewPagerFragmentDirections
import dev.gtcl.astro.ui.fragments.account.pages.UserListAdapter
import dev.gtcl.astro.ui.fragments.inbox.ComposeDialogFragment

class FriendsFragment : Fragment(), UserActions{

    private lateinit var binding: FragmentItemScrollerBinding

    val model: FriendsVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(FriendsVM::class.java)
    }

    override fun onResume() {
        super.onResume()
        val scrollPosition = binding.fragmentItemScrollerList.scrollY
        if(scrollPosition == 0){
            binding.fragmentItemScrollerList.scrollToPosition(0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)

        if(model.friends.value == null){
            model.getFriends()
        }

        val adapter = UserListAdapter(UserType.FRIEND, model::getFriends, this)
        binding.fragmentItemScrollerList.adapter = adapter

        model.networkState.observe(viewLifecycleOwner, {
            adapter.networkState = it
            if(it == NetworkState.LOADED){
                binding.fragmentItemScrollerSwipeRefresh.isRefreshing = false
            }
        })

        model.friends.observe(viewLifecycleOwner, {
            adapter.submitList(it)
            binding.fragmentItemScrollerList.scrollToPosition(0)
        })

        binding.fragmentItemScrollerSwipeRefresh.setOnRefreshListener {
            model.getFriends()
        }

        model.removeAt.observe(viewLifecycleOwner, {
            if(it != null){
                adapter.removeAt(it)
                model.removeAtObserved()
            }
        })

        return binding.root
    }

    override fun viewProfile(user: User) {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(user.name)))
    }

    override fun message(user: User) {
        ComposeDialogFragment.newInstance(user.name).show(childFragmentManager, null)
    }

    override fun remove(position: Int) {
        model.removeAndUnfriendAt(position)
    }


    companion object{
        fun newInstance() = FriendsFragment()
    }
}