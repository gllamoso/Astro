package dev.gtcl.reddit.ui.fragments.subreddits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.actions.ListingTypeClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.actions.SubscriptionActions
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.databinding.FragmentDialogSubscriptionsBinding
import dev.gtcl.reddit.models.reddit.ListingType
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.ViewPagerFragmentDirections

class SubscriptionsDialogFragment: BottomSheetDialogFragment(), SubscriptionActions, ListingTypeClickListener{

    private lateinit var binding: FragmentDialogSubscriptionsBinding
    private var parentListingTypeClickListener: ListingTypeClickListener? = null
    private var parentSubredditActions: SubredditActions? = null

    fun setActions(listingTypeClickListener: ListingTypeClickListener, subredditActions: SubredditActions){
        this.parentListingTypeClickListener = listingTypeClickListener
        this.parentSubredditActions = subredditActions
    }

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
        binding.recyclerView.adapter = adapter

        if(activityModel.refreshState.value == null){
            model.fetchSubscriptions()
        }

        activityModel.refreshState.observe(viewLifecycleOwner, Observer {
            if(it == NetworkState.LOADED){
                model.fetchSubscriptions()
                activityModel.refreshObserved()
            }
        })

        model.favorites.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.setFavorites(it)
                binding.recyclerView.scrollToPosition(0)
                model.favoritesObserved()
            }
        })

        model.multireddits.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.setMultiReddits(it)
                model.multiredditsObserved()
            }
        })

        model.subreddits.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.setSubscribedSubs(it)
                model.subredditsObserved()
            }
        })

        model.users.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.setUsers(it)
                model.usersObserved()
            }
        })

        activityModel.refreshState.observe(viewLifecycleOwner, Observer {
            binding.progressBar.visibility = if(it == NetworkState.LOADING) View.VISIBLE else View.GONE
        })
    }

    private fun setListeners(){
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.search ->  {
                    findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentToSearchFragment(false))
                    dismiss()
                }
                R.id.sync -> activityModel.syncSubscriptionsWithReddit()
            }
            true
        }
    }

    override fun listingTypeClicked(listing: ListingType) {
        parentListingTypeClickListener?.listingTypeClicked(listing)
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