package dev.gtcl.reddit.ui.fragments.account.pages.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.actions.UserActions
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding
import dev.gtcl.reddit.models.reddit.User
import dev.gtcl.reddit.models.reddit.UserType
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.fragments.AccountPage
import dev.gtcl.reddit.ui.fragments.ViewPagerFragmentDirections
import dev.gtcl.reddit.ui.fragments.account.pages.UserListAdapter
import dev.gtcl.reddit.ui.fragments.inbox.ComposeDialogFragment

class FriendsFragment : Fragment(), UserActions{

    private lateinit var binding: FragmentItemScrollerBinding

    val model: FriendsVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(FriendsVM::class.java)
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