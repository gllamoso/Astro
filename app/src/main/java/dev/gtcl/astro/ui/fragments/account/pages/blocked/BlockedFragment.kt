package dev.gtcl.astro.ui.fragments.account.pages.blocked

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.actions.UserActions
import dev.gtcl.astro.databinding.FragmentItemScrollerBinding
import dev.gtcl.astro.models.reddit.User
import dev.gtcl.astro.models.reddit.UserType
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.ui.fragments.view_pager.AccountPage
import dev.gtcl.astro.ui.fragments.account.pages.UserListAdapter
import dev.gtcl.astro.ui.fragments.view_pager.ViewPagerFragmentDirections

class BlockedFragment : Fragment(), UserActions {

    private var binding: FragmentItemScrollerBinding? = null

    val model: BlockedVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(BlockedVM::class.java)
    }

    override fun onResume() {
        super.onResume()
        val scrollPosition = binding?.fragmentItemScrollerList?.scrollY
        if (scrollPosition == 0) {
            binding?.fragmentItemScrollerList?.scrollToPosition(0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)

        if (model.blocked.value == null) {
            model.getBlocked()
        }
        val adapter = UserListAdapter(UserType.BLOCKED, model::getBlocked, this)
        binding?.fragmentItemScrollerList?.adapter = adapter

        model.networkState.observe(viewLifecycleOwner, {
            adapter.networkState = it
            if (it == NetworkState.LOADED) {
                binding?.fragmentItemScrollerSwipeRefresh?.isRefreshing = false
            }
        })

        model.blocked.observe(viewLifecycleOwner, {
            adapter.submitList(it)
            binding?.fragmentItemScrollerList?.scrollToPosition(0)
        })

        binding?.fragmentItemScrollerSwipeRefresh?.setOnRefreshListener {
            model.getBlocked()
        }

        model.removeAt.observe(viewLifecycleOwner, {
            if (it != null) {
                adapter.removeAt(it)
                model.removeAtObserved()
            }
        })

        model.errorMessage.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                binding?.root?.let{
                    Snackbar.make(it, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                model.errorMessageObserved()
            }
        })

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun viewProfile(user: User) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(user.name)
            )
        )
    }

    override fun message(user: User) {}

    override fun remove(position: Int) {
        model.removeAndUnblockAt(position)
    }

    companion object {
        fun newInstance() = BlockedFragment()
    }
}