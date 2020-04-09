package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.listings.*
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.ListingOnClickListeners

class MineFragment : Fragment() {

    private lateinit var binding: FragmentRecyclerViewBinding
    private lateinit var subClickListener: ListingOnClickListeners

    fun setFragment(listener: ListingOnClickListeners){
        this.subClickListener = listener
    }

    val model: MineViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity(). application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MineViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecyclerViewBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val adapter = MultiAndSubsListAdapter(requireContext(), subClickListener)
        binding.list.adapter = adapter

        model.subscribedSubs.observe(viewLifecycleOwner, Observer {
            val multis = mutableListOf(FrontPage, All, Popular) // TODO
            if((requireActivity().application as RedditApplication).accessToken != null) multis.add(ProfileListing(ProfileInfo.SAVED))
            adapter.submitLists(multis, it)
        })

        model.favoriteSubs.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.submitFavorites(it)
            }
        })
    }

    fun syncSubscribedSubs(){
        model.syncSubscribedSubs()
    }


}
