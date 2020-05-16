package dev.gtcl.reddit.ui.fragments.subreddits.trending

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubscribeAction
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.fragments.ListingScrollListener

class TrendingListFragment : Fragment(), SubredditActions, ItemClickListener{

    private lateinit var binding: FragmentItemScrollerBinding

    val model: TrendingListViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(TrendingListViewModel::class.java)
    }

    private val listAdapter: TrendingAdapter by lazy{
        TrendingAdapter(this, model::retry)
    }

    private val listingScrollListener by lazy{
        ListingScrollListener(loadMore = model::loadAfter)
    }

    private var parentItemClickListener: ItemClickListener? = null

    fun setItemClickListener(listener: ItemClickListener){
        parentItemClickListener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)
        binding.nestedScrollView.setOnScrollChangeListener(listingScrollListener)
        listAdapter.itemClickListener = this
        binding.list.adapter = listAdapter
        setSwipeRefresh()
        setObservers()
        if(!model.initialPageLoaded){
            model.loadInitialDataAndFirstPage()
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
            binding.progressBar.visibility = if(it == NetworkState.LOADING) View.VISIBLE else View.GONE
            listAdapter.networkState = it
        })

        model.favoriteSubs.observe(viewLifecycleOwner, Observer {
            if(it != null && it.isNotEmpty()) {
                listAdapter.updateFavoriteItems(it)
            }
        })

        model.subscribedSubs.observe(viewLifecycleOwner, Observer {
            if(it != null && it.isNotEmpty()){
                listAdapter.updateSubscribedItems(it)
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

//    private val trendingAdapter: TrendingAdapter by lazy {
//        Tre
//    }

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun addToFavorites(subreddit: Subreddit, favorite: Boolean) {
        model.addToFavorites(subreddit, favorite)
//        if(refresh) refreshMineFragment()
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        model.subscribe(subreddit, if(subscribe) SubscribeAction.SUBSCRIBE else SubscribeAction.UNSUBSCRIBE, false)
//        if(refresh) refreshMineFragment()
    }

    companion object{
        fun newInstance(): TrendingListFragment{
            return TrendingListFragment()
        }
    }

    override fun itemClicked(item: Item) {
        parentItemClickListener?.itemClicked(item)
    }

}