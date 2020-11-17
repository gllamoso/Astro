package dev.gtcl.astro.ui.fragments.account.pages.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.AstroApplication
import dev.gtcl.astro.USER_KEY
import dev.gtcl.astro.ViewModelFactory
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.MultiRedditActions
import dev.gtcl.astro.actions.SubredditActions
import dev.gtcl.astro.createBetterLinkMovementInstance
import dev.gtcl.astro.databinding.FragmentAccountAboutBinding
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.account.pages.about.dialogs.MultiRedditInfoDialogFragment
import dev.gtcl.astro.ui.fragments.subreddits.SubredditInfoDialogFragment
import dev.gtcl.astro.ui.fragments.view_pager.AccountPage
import dev.gtcl.astro.ui.fragments.view_pager.ListingPage
import dev.gtcl.astro.ui.fragments.view_pager.ViewPagerFragmentDirections

class AccountAboutFragment : Fragment(), SubredditActions, MultiRedditActions,
    ItemClickListener {

    private var binding: FragmentAccountAboutBinding? = null

    private val activityModel: MainActivityVM by activityViewModels()

    val model: AccountAboutVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(AccountAboutVM::class.java)
    }

    private val movementMethod by lazy {
        createBetterLinkMovementInstance(requireContext(), findNavController(), parentFragmentManager, activityModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAccountAboutBinding.inflate(inflater)
        binding?.lifecycleOwner = this
        binding?.model = model
        val user = arguments?.getString(USER_KEY)
        model.fetchAccount(user)
        if ((requireActivity().application as AstroApplication).currentAccount != null) {
            model.fetchTrophies(user)
        }
        model.fetchModeratedSubs(user)
        model.fetchPublicFeeds(user)

        val trophiesAdapter = TrophyAdapter()
        model.trophies.observe(viewLifecycleOwner, {
            trophiesAdapter.submitList(it)
        })

        val moderatedSubsAdapter = ModeratedSubsAdapter(this, this)
        model.moderatedSubs.observe(viewLifecycleOwner, {
            moderatedSubsAdapter.submitList(it)
        })

        val multiRedditsAdapter = MultiRedditsAdapter(this, this, movementMethod)
        model.multiReddits.observe(viewLifecycleOwner, {
            multiRedditsAdapter.submitList(it)
        })

        model.errorMessage.observe(viewLifecycleOwner, {
            if (it != null) {
                Snackbar.make((binding ?: return@observe).root, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        binding?.apply {
            fragmentAccountAboutTrophiesList.adapter = trophiesAdapter
            fragmentAccountAboutModeratedSubs.adapter = moderatedSubsAdapter
            fragmentAccountAboutMultis.adapter = multiRedditsAdapter
        }

        activityModel.newMulti.observe(viewLifecycleOwner, {
            if (it != null) {
                findNavController().navigate(
                    ViewPagerFragmentDirections.actionViewPagerFragmentToMultiRedditFragment(it.pathFormatted)
                )
                activityModel.newMultiObserved()
            }
        })

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun viewMoreInfo(displayName: String) {
        if (displayName.startsWith("u_")) {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                    AccountPage(displayName.removePrefix("u_"))
                )
            )
        } else {
            SubredditInfoDialogFragment.newInstance(displayName).show(childFragmentManager, null)
        }
    }

    override fun viewMoreInfo(multi: MultiReddit) {
        MultiRedditInfoDialogFragment.newInstance(multi).show(childFragmentManager, null)
    }

    override fun itemClicked(item: Item, position: Int) {
        when (item) {
            is SubredditInModeratedList -> {
                findNavController().navigate(
                    ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                        ListingPage(SubredditListing(item.displayName))
                    )
                )
            }
            is MultiReddit -> {
                findNavController().navigate(
                    ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                        ListingPage(MultiRedditListing(item.displayName, item.pathFormatted))
                    )
                )
            }
        }
    }

    override fun itemLongClicked(item: Item, position: Int) {} // Unused

    companion object {
        fun newInstance(user: String?): AccountAboutFragment {
            val fragment = AccountAboutFragment()
            val args = bundleOf(USER_KEY to user)
            fragment.arguments = args
            return fragment
        }
    }
}