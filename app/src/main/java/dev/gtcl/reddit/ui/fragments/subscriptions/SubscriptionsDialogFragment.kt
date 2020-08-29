package dev.gtcl.reddit.ui.fragments.subscriptions

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.ListingTypeClickListener
import dev.gtcl.reddit.actions.SubscriptionActions
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.databinding.FragmentDialogSubscriptionsBinding
import dev.gtcl.reddit.databinding.PopupSubscriptionActionsBinding
import dev.gtcl.reddit.models.reddit.listing.Listing
import dev.gtcl.reddit.models.reddit.listing.MultiRedditUpdate
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.ViewPagerFragmentDirections
import dev.gtcl.reddit.ui.fragments.multireddits.MultiRedditDetailsDialogFragment

class SubscriptionsDialogFragment: BottomSheetDialogFragment(), SubscriptionActions, ListingTypeClickListener{

    private lateinit var binding: FragmentDialogSubscriptionsBinding

    private val activityModel: MainActivityVM by activityViewModels()

    val model: SubscriptionsVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SubscriptionsVM::class.java)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDialogSubscriptionsBinding.inflate(inflater)
        setRecyclerView()
        setListeners()
        return binding.root
    }

    private fun setRecyclerView(){
        val adapter =
            SubscriptionsAdapter(
                requireContext(),
                this,
                this
            )
        binding.fragmentDialogSubscriptionsRecyclerView.adapter = adapter

        if(activityModel.refreshState.value == null){
            model.fetchSubscriptions()
        }

        activityModel.refreshState.observe(viewLifecycleOwner, {
            if(it == NetworkState.LOADED){
                model.fetchSubscriptions()
                activityModel.refreshObserved()
            }
        })

        model.subscriptions.observe(viewLifecycleOwner, {
            if(it != null){
                adapter.setSubscriptions(it.favorites, it.multiReddits, it.subreddits, it.users)
                model.subscriptionsObserved()
            }
        })

        activityModel.refreshState.observe(viewLifecycleOwner, {
            binding.fragmentDialogSubscriptionsProgressBar.visibility = if(it == NetworkState.LOADING) View.VISIBLE else View.GONE
        })
    }

    private fun setListeners(){
        binding.fragmentDialogSubscriptionsToolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.fragmentDialogSubscriptionsSearch.setOnClickListener {
            findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSearchFragment(false))
            dismiss()
        }

        binding.fragmentDialogSubscriptionsSync.setOnClickListener {
            activityModel.syncSubscriptionsWithReddit()
        }

        binding.fragmentDialogSubscriptionsMoreOptions.setOnClickListener {
            showMoreOptionsPopupWindow(it)
        }

        model.errorMessage.observe(viewLifecycleOwner, {
            if(it != null){
                AlertDialog.Builder(requireContext())
                    .setMessage(it)
                    .setPositiveButton(getString(R.string.done)){ dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                model.errorMessageObserved()
            }
        })

        childFragmentManager.setFragmentResultListener(MULTI_KEY, viewLifecycleOwner, FragmentResultListener{ _, bundle ->
            val multiUpdate = bundle.get(MULTI_KEY) as MultiRedditUpdate
            model.createMulti(multiUpdate)
        })

        model.editSubscription.observe(viewLifecycleOwner, {
            if(it != null){
                editMultiReddit(it)
            }
        })
    }

    private fun showMoreOptionsPopupWindow(anchor: View){
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupSubscriptionActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            popupSubscriptionActionsCreateCustomFeed.root.setOnClickListener {
                MultiRedditDetailsDialogFragment.newInstance(null).show(childFragmentManager, null)
                popupWindow.dismiss()
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        popupWindow.showAsDropdown(anchor, popupBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, popupBinding.root.measuredHeight)
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

    override fun favorite(sub: Subscription, favorite: Boolean) {
        activityModel.favorite(sub, favorite)
    }

    override fun remove(sub: Subscription) {
        activityModel.unsubscribe(sub)
    }

    override fun editMultiReddit(sub: Subscription) {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToMultiRedditFragment(sub))
        dismiss()
    }

    companion object{
        fun newInstance(): SubscriptionsDialogFragment {
            return SubscriptionsDialogFragment()
        }
    }

}