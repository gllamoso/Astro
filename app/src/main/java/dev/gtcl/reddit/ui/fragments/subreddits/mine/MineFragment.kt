package dev.gtcl.reddit.ui.fragments.subreddits.mine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.actions.ListingActions
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding

class MineFragment : Fragment() {

    private lateinit var binding: FragmentItemScrollerBinding
    private lateinit var listingActions: ListingActions
    private lateinit var subredditActions: SubredditActions

    fun setFragment(listingActions: ListingActions, subActions: SubredditActions){
        this.listingActions = listingActions
        this.subredditActions = subActions
    }

    val model: MineViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity(). application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MineViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)
        model.loadInitial()
        setRecyclerViewAdapter()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if(model.refresh){
            model.loadInitial()
            model.refresh = false
        }
    }

    private fun setRecyclerViewAdapter(){
        val adapter = MultiAndSubredditsAdapter(requireContext(), listingActions, subredditActions)
        binding.list.adapter = adapter

        model.initialSubs.observe(viewLifecycleOwner, Observer {
            if(it != null) {
                for(sub: Subreddit in it)
                    sub.userSubscribed = true
                adapter.loadInitialSubreddits(it)
                model.initialLoadFinished()
            }
        })
    }

    fun syncSubscribedSubs(){
        model.syncSubscribedSubs()
    }

    fun refresh(){
        model.refresh = true
    }

    companion object{
        fun newInstance(): MineFragment{
            return MineFragment()
        }
    }
}
