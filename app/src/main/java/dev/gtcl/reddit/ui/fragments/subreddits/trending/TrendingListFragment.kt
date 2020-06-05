package dev.gtcl.reddit.ui.fragments.subreddits.trending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.ListingTypeClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditListing
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.ItemScrollListener

class TrendingListFragment : Fragment(), SubredditActions, ItemClickListener{

    private lateinit var binding: FragmentItemScrollerBinding

    val model: TrendingListVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(TrendingListVM::class.java)
    }

    private val listAdapter: TrendingAdapter by lazy{
        TrendingAdapter(this,this, model::retry)
    }

    private val listingScrollListener by lazy{
        ItemScrollListener(7, binding.list.layoutManager as GridLayoutManager, model::loadAfter)
    }

    private var parentListingTypeClickListener: ListingTypeClickListener? = null
    private var parentSubredditActions: SubredditActions? = null
    fun setActions(listingTypeClickListener: ListingTypeClickListener, subredditActions: SubredditActions){
        parentListingTypeClickListener = listingTypeClickListener
        parentSubredditActions = subredditActions
    }

    override fun onResume() {
        super.onResume()
        model.syncWithDb()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)
        binding.list.adapter = listAdapter
        binding.list.addOnScrollListener(listingScrollListener)
        setSwipeRefresh()
        setObservers()
        if(!model.initialPageLoaded){
            model.loadInitialDataAndFirstPage()
            model.initialPageLoaded = true
        }
        return binding.root
    }

    private fun setObservers(){

        model.items.observe(viewLifecycleOwner, Observer {
            if(it != null){
                listAdapter.clearItems()
                listAdapter.addItems(it)
                listingScrollListener.finishedLoading()
                if(it.isEmpty()){
                    binding.list.visibility = View.GONE
                    binding.noResultsText.visibility = View.VISIBLE
                } else {
                    binding.list.visibility = View.VISIBLE
                    binding.noResultsText.visibility = View.GONE
                }
            }
        })

        model.newItems.observe(viewLifecycleOwner, Observer {
            if(it != null){
                listAdapter.addItems(it)
                model.newItemsAdded()
                listingScrollListener.finishedLoading()
            }
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
//            binding.progressBar.visibility = if(it == NetworkState.LOADING){
//                View.VISIBLE
//            } else {
//                View.GONE
//            }
            listAdapter.networkState = it
        })

        model.subscribedSubs.observe(viewLifecycleOwner, Observer {
            if(it != null){
                listAdapter.updateSubscribedItems(it)
                model.subredditsSynced()
            }
        })
    }

    private fun setSwipeRefresh(){
        binding.swipeRefresh.setOnRefreshListener {
            model.refresh()
        }

        model.refreshState.observe(viewLifecycleOwner, Observer {
            if(it == NetworkState.LOADED){
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        parentSubredditActions?.subscribe(subreddit, subscribe)
    }

    companion object{
        fun newInstance(): TrendingListFragment{
            return TrendingListFragment()
        }
    }

    override fun itemClicked(item: Item) {
        if(item is Subreddit){
            parentListingTypeClickListener?.listingTypeClicked(SubredditListing(item))
        }
    }

}