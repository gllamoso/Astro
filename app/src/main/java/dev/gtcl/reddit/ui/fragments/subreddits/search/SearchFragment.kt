package dev.gtcl.reddit.ui.fragments.subreddits.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.database.asDomainModel
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.network.NetworkState

class SearchFragment : Fragment(), ItemClickListener, SubredditActions {
    private lateinit var binding: FragmentItemScrollerBinding

    private val model: SearchViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SearchViewModel::class.java)
    }

    private val searchAdapter by lazy{
        SearchAdapter(this, this)
    }

    private var parentSubredditActions: SubredditActions? = null
    private var parentItemClickListener: ItemClickListener? = null

    fun setActions(subredditActions: SubredditActions, itemClickListener: ItemClickListener){
        parentSubredditActions = subredditActions
        parentItemClickListener = itemClickListener
    }

    override fun onResume() {
        super.onResume()
        model.syncWithDb()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)
        binding.list.visibility = View.GONE
        binding.noResultsText.visibility = View.VISIBLE
//        binding.progressBar.visibility = View.GONE
        setRecyclerViewAdapter()
        return binding.root
    }

    private fun setRecyclerViewAdapter(){
        binding.list.adapter = searchAdapter

        model.searchedSubreddits.observe(viewLifecycleOwner, Observer {
            if(it != null){
                searchAdapter.submitList(it)
                binding.list.smoothScrollToPosition(0)
                binding.list.visibility = if(it.isEmpty()) View.GONE else View.VISIBLE
                binding.noResultsText.visibility = if(it.isEmpty()) View.VISIBLE else View.GONE
                model.searchComplete()
            }
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
//            binding.progressBar.visibility = if(it == NetworkState.LOADED){
//                View.GONE
//            } else {
//                View.VISIBLE
//            }
        })

        model.subscribedSubs.observe(viewLifecycleOwner, Observer {
            if(it != null){
                searchAdapter.updateSubscribedItems(it)
                model.subredditsSynced()
            }
        })

        model.favoriteSubs.observe(viewLifecycleOwner, Observer {
            if(it != null){
                searchAdapter.updateFavoriteItems(it)
                model.favoriteSubsSynced()
            }
        })
    }

    fun searchSubreddit(query: String){
        searchAdapter.submitList(listOf())
        model.searchSubreddits(query)
    }

    companion object{
        fun newInstance(): SearchFragment{
            return SearchFragment()
        }
    }

    override fun itemClicked(item: Item) {
        parentItemClickListener?.itemClicked(item)
    }

    override fun favorite(subreddit: Subreddit, favorite: Boolean) {
        parentSubredditActions?.favorite(subreddit, favorite)
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        parentSubredditActions?.favorite(subreddit, subscribe)
    }
}