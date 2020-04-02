package dev.gtcl.reddit.ui.fragments.home.subreddits.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.listings.*
import dev.gtcl.reddit.ui.fragments.home.HomeFragment
import dev.gtcl.reddit.ui.fragments.home.subreddits.SubredditOnClickListener

class MineFragment : Fragment() {

    private lateinit var binding: FragmentRecyclerViewBinding
    private lateinit var subClickListener: SubredditOnClickListener

    fun setSubredditOnClickListener(listener: SubredditOnClickListener){
        this.subClickListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecyclerViewBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val model = (requireParentFragment().parentFragment as HomeFragment).model

        val adapter = MultiAndSubsListAdapter(requireContext(), subClickListener)
        binding.list.adapter = adapter

        model.defaultSubreddits.observe(viewLifecycleOwner, Observer {
            val multis = mutableListOf(FrontPage, All, Popular) // TODO
            if((requireActivity().application as RedditApplication).accessToken != null) multis.add(ProfileListing(ProfileInfo.SAVED))
            adapter.submitLists(multis, it)
        })
    }

}
