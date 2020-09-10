package dev.gtcl.astro.ui.fragments.subscriptions

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.ListingTypeClickListener
import dev.gtcl.astro.actions.SubscriptionActions
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.databinding.FragmentDialogSubscriptionsBinding
import dev.gtcl.astro.databinding.PopupSubscriptionActionsBinding
import dev.gtcl.astro.models.reddit.listing.Listing
import dev.gtcl.astro.models.reddit.listing.MultiRedditUpdate
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.multireddits.MultiRedditDetailsDialogFragment
import dev.gtcl.astro.ui.fragments.view_pager.ViewPagerFragmentDirections

class SubscriptionsDialogFragment : BottomSheetDialogFragment(), SubscriptionActions,
    ListingTypeClickListener {

    private var binding: FragmentDialogSubscriptionsBinding? = null

    private val activityModel: MainActivityVM by activityViewModels()

    val model: SubscriptionsVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(SubscriptionsVM::class.java)
    }

    val adapter: SubscriptionsAdapter by lazy {
        SubscriptionsAdapter(
            requireContext(),
            this,
            this
        )
    }

    override fun onStart() {
        super.onStart()

        dialog?.let {
            val bottomSheet = it.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.peekHeight = resources.displayMetrics.heightPixels
            view?.requestLayout()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDialogSubscriptionsBinding.inflate(inflater)
        setRecyclerView()
        setListeners()
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Glide.get(requireContext()).clearMemory()
        binding = null
    }

    private fun setRecyclerView() {
        binding?.fragmentDialogSubscriptionsRecyclerView?.adapter = adapter

        if (activityModel.refreshState.value == null) {
            model.fetchSubscriptions()
        }

        activityModel.refreshState.observe(viewLifecycleOwner, {
            if (it == NetworkState.LOADED) {
                model.fetchSubscriptions()
                activityModel.refreshObserved()
            }
        })

        model.subscriptions.observe(viewLifecycleOwner, {
            if (it != null) {
                adapter.setSubscriptions(it.favorites, it.multiReddits, it.subreddits, it.users)
                model.subscriptionsObserved()
            }
        })

        activityModel.refreshState.observe(viewLifecycleOwner, {
            binding?.fragmentDialogSubscriptionsProgressBar?.visibility =
                if (it == NetworkState.LOADING) View.VISIBLE else View.GONE
        })
    }

    private fun setListeners() {
        binding?.fragmentDialogSubscriptionsToolbar?.setNavigationOnClickListener {
            dismiss()
        }

        binding?.fragmentDialogSubscriptionsSearch?.setOnClickListener {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentToSearchFragment(
                    false
                )
            )
            dismiss()
        }

        binding?.fragmentDialogSubscriptionsSync?.setOnClickListener {
            activityModel.syncSubscriptionsWithReddit()
        }

        binding?.fragmentDialogSubscriptionsMoreOptions?.setOnClickListener {
            showMoreOptionsPopupWindow(it)
        }

        model.errorMessage.observe(viewLifecycleOwner, {
            if (it != null) {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        childFragmentManager.setFragmentResultListener(MULTI_KEY, viewLifecycleOwner, { _, bundle ->
            val multiUpdate = bundle.get(MULTI_KEY) as MultiRedditUpdate
            model.createMulti(multiUpdate)
        })

        model.editSubscription.observe(viewLifecycleOwner, {
            if (it != null) {
                editMultiReddit(it)
            }
        })
    }

    private fun showMoreOptionsPopupWindow(anchor: View) {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupSubscriptionActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            popupSubscriptionActionsCreateCustomFeed.root.setOnClickListener {
                checkedIfLoggedInBeforeExecuting(requireContext()) {
                    MultiRedditDetailsDialogFragment.newInstance(null)
                        .show(childFragmentManager, null)
                }
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        popupWindow.showAsDropdown(
            anchor,
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupBinding.root.measuredHeight
        )
    }

    override fun listingTypeClicked(listing: Listing) {
        parentFragmentManager.setFragmentResult(LISTING_KEY, bundleOf(LISTING_KEY to listing))
        dismiss()
    }

//      _____       _                   _       _   _                            _   _
//     / ____|     | |                 (_)     | | (_)                 /\       | | (_)
//    | (___  _   _| |__  ___  ___ _ __ _ _ __ | |_ _  ___  _ __      /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \/ __|/ __| '__| | '_ \| __| |/ _ \| '_ \    / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) \__ \ (__| |  | | |_) | |_| | (_) | | | |  / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|___/\___|_|  |_| .__/ \__|_|\___/|_| |_| /_/    \_\___|\__|_|\___/|_| |_|___/
//                                       | |
//                                       |_|

    override fun favorite(sub: Subscription, favorite: Boolean, inFavoritesSection: Boolean) {
        sub.isFavorite = favorite
        if (favorite) {
            adapter.addToFavorites(sub)
        } else {
            adapter.removeFromFavorites(sub, inFavoritesSection)
        }
        activityModel.favorite(sub, favorite)
    }

    override fun remove(sub: Subscription) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.unsubscribe(sub)
        }
    }

    override fun editMultiReddit(sub: Subscription) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentToMultiRedditFragment(
                    sub
                )
            )
            dismiss()
        }
    }

    companion object {
        fun newInstance(): SubscriptionsDialogFragment {
            return SubscriptionsDialogFragment()
        }
    }

}