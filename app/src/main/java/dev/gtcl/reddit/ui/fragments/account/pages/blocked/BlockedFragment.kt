package dev.gtcl.reddit.ui.fragments.account.pages.blocked

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.USER_KEY
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.actions.UserActions
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding
import dev.gtcl.reddit.models.reddit.User
import dev.gtcl.reddit.models.reddit.UserType
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.fragments.AccountPage
import dev.gtcl.reddit.ui.fragments.ViewPagerFragmentDirections
import dev.gtcl.reddit.ui.fragments.account.pages.UserListAdapter
import dev.gtcl.reddit.ui.fragments.account.pages.friends.FriendsVM

class BlockedFragment : Fragment(), UserActions {

    private lateinit var binding: FragmentItemScrollerBinding

    val model: BlockedVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(BlockedVM::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)

        if(model.blocked.value == null){
            model.getBlocked()
        }
        val adapter = UserListAdapter(UserType.BLOCKED, model::getBlocked, this)
        binding.list.adapter = adapter

        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.networkState = it
            if(it == NetworkState.LOADED){
                binding.swipeRefresh.isRefreshing = false
            }
        })

        model.blocked.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

        binding.swipeRefresh.setOnRefreshListener {
            model.getBlocked()
        }

        model.removeAt.observe(viewLifecycleOwner, Observer {
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

    override fun message(user: User) {}

    override fun remove(position: Int) {
        model.removeAndUnblockAt(position)
    }

    companion object{
        fun newInstance() = BlockedFragment()
    }
}