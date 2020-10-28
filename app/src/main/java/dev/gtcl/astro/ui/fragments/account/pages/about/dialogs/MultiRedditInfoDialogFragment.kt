package dev.gtcl.astro.ui.fragments.account.pages.about.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import dev.gtcl.astro.MULTI_KEY
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.FragmentDialogMultiredditInfoBinding
import dev.gtcl.astro.models.reddit.listing.Item
import dev.gtcl.astro.models.reddit.listing.MultiReddit
import dev.gtcl.astro.models.reddit.listing.Subreddit
import dev.gtcl.astro.models.reddit.listing.SubredditListing
import dev.gtcl.astro.ui.fragments.multireddits.MultiRedditCreationDialogFragment
import dev.gtcl.astro.ui.fragments.multireddits.MultiRedditSubredditsAdapter
import dev.gtcl.astro.ui.fragments.view_pager.ListingPage
import dev.gtcl.astro.ui.fragments.view_pager.ViewPagerFragmentDirections

class MultiRedditInfoDialogFragment : DialogFragment(), ItemClickListener {

    private var binding: FragmentDialogMultiredditInfoBinding? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
//        dialog?.window?.setBackgroundDrawableResource(android.R.color.black) // This makes the dialog full screen
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogMultiredditInfoBinding.inflate(inflater)
        val multi = requireArguments().get(MULTI_KEY) as MultiReddit
        val adapter = MultiRedditSubredditsAdapter(this, null)
        adapter.submitList(multi.subreddits.mapNotNull { it.data })
        binding?.title = multi.displayName
        binding?.fragmentDialogMultiRedditInfoSubredditList?.adapter = adapter
        binding?.executePendingBindings()

        binding?.fragmentDialogMultiRedditInfoDialogButtons?.dialogPositiveButton?.setOnClickListener {
            dismiss()
        }

        binding?.fragmentDialogMultiRedditInfoToolbar?.setOnMenuItemClickListener {
            MultiRedditCreationDialogFragment.newInstance(multi, true)
                .show(parentFragmentManager, null)
            dismiss()
            true
        }

        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        fun newInstance(multi: MultiReddit): MultiRedditInfoDialogFragment {
            return MultiRedditInfoDialogFragment().apply {
                arguments = bundleOf(MULTI_KEY to multi)
            }
        }
    }

    override fun clicked(item: Item, position: Int) {
        if (item is Subreddit) {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                    ListingPage(SubredditListing(item.displayName))
                )
            )
            dismiss()
        }
    }

    override fun longClicked(item: Item, position: Int) {} // Unused
}