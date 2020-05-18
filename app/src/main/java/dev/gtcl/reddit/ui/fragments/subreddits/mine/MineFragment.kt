package dev.gtcl.reddit.ui.fragments.subreddits.mine

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.*
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.actions.ListingTypeClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding
import dev.gtcl.reddit.models.reddit.ListingType

class MineFragment : Fragment(), SubredditActions, ListingTypeClickListener {

    private lateinit var binding: FragmentItemScrollerBinding
    private var parentListingTypeClickListener: ListingTypeClickListener? = null
    private var parentSubredditActions: SubredditActions? = null

    fun setActions(listingTypeClickListener: ListingTypeClickListener, subredditActions: SubredditActions){
        this.parentListingTypeClickListener = listingTypeClickListener
        this.parentSubredditActions = subredditActions
    }

    val model: MineViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity(). application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MineViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        val adapter = MySubredditsAdapter(requireContext(), this, this)
        binding.list.adapter = adapter

        model.subscribedSubs.observe(requireParentFragment().viewLifecycleOwner, Observer {
            if(it != null
                && (!model.subscribedInitialized || lifecycle.currentState != Lifecycle.State.RESUMED)) {
                for(sub: Subreddit in it){
                    sub.userSubscribed = true
                }
                adapter.setSubscribedSubs(it)
                model.subscribedInitialized = true
                binding.progressBar.visibility = View.GONE
            }
        })

        model.multiReddits.observe(requireParentFragment().viewLifecycleOwner, Observer {
            if(it != null
                && (!model.multiRedditsInitialized || lifecycle.currentState != Lifecycle.State.RESUMED)){
                adapter.setMultiReddits(it)
                model.multiRedditsInitialized = true
            }
        })
    }

    companion object{
        fun newInstance(): MineFragment{
            return MineFragment()
        }
    }

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun favorite(subreddit: Subreddit, favorite: Boolean) {
        parentSubredditActions?.favorite(subreddit, favorite)
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        parentSubredditActions?.subscribe(subreddit, subscribe)
    }

    override fun onClick(listing: ListingType) {
        parentListingTypeClickListener?.onClick(listing)
    }
}
