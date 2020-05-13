package dev.gtcl.reddit.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.MessageActions
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.database.asDomainModel
import dev.gtcl.reddit.databinding.FragmentRecyclerViewBinding
import dev.gtcl.reddit.models.reddit.ProfileListing
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditListing
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.ListingAdapter
import dev.gtcl.reddit.ui.LoadMoreScrollListener
import dev.gtcl.reddit.ui.fragments.dialog.subreddits.trending.TrendingAdapter
import dev.gtcl.reddit.ui.fragments.dialog.subreddits.trending.toTrendingPosts

open class SimpleListingScrollerFragment : Fragment(){

    private lateinit var binding: FragmentRecyclerViewBinding

    val model: LoadMoreScrollViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(LoadMoreScrollViewModel::class.java)
    }

    private var postActions: PostActions? = null
    private var messageActions: MessageActions? = null
    private var subredditActions: SubredditActions? = null
    fun setActions(postActions: PostActions? = null, messageActions: MessageActions? = null, subredditActions: SubredditActions? = null, user: String? = null){
        this.postActions = postActions
        this.messageActions = messageActions
        this.subredditActions = subredditActions
        setListingInfo()
        model.setUser(user)
        model.loadInitial()
    }

    private fun setListingInfo(){
        val args = requireArguments()
        when{
            args.getSerializable(PROFILE_INFO_KEY) != null -> {
                val profileInfo = args.getSerializable(PROFILE_INFO_KEY) as ProfileInfo
                val postSort = args.getSerializable(POST_SORT_KEY) as PostSort
                val time = args.getSerializable(TIME_KEY) as Time?
                val pageSize = args.getInt(PAGE_SIZE_KEY)
                model.setListingInfo(ProfileListing(profileInfo), postSort, time, pageSize)
            }
            args.getParcelable<Subreddit>(SUBREDDIT_KEY) != null -> {
                val subreddit = args.getParcelable<Subreddit>(SUBREDDIT_KEY)!!
                val postSort = args.getSerializable(POST_SORT_KEY) as PostSort
                val time = args.getSerializable(TIME_KEY) as Time?
                val pageSize = args.getInt(PAGE_SIZE_KEY)
                model.setListingInfo(SubredditListing(subreddit), postSort, time, pageSize)
            }
            args.getSerializable(MESSAGE_WHERE_KEY) != null -> {
                val messageWhere = args.getSerializable(MESSAGE_WHERE_KEY) as MessageWhere
                val pageSize = args.getInt(PAGE_SIZE_KEY)
                model.setListingInfo(messageWhere, pageSize)
            }
            args.getSerializable(SUBREDDIT_WHERE_KEY) != null -> {
                val subredditWhere = args.getSerializable(SUBREDDIT_WHERE_KEY) as SubredditWhere
                val pageSize = args.getInt(PAGE_SIZE_KEY)
                model.setListingInfo(subredditWhere, pageSize)
            }
            else -> throw IllegalStateException("Missing key arguments")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecyclerViewBinding.inflate(inflater)
        binding.list.adapter = if(arguments?.getBoolean(USE_TRENDING_ADAPTER_KEY) == true){
                createTrendingAdapter()
            }
            else {
                createListingAdapter()
            }
        setSwipeRefresh()
        return binding.root
    }

    private fun createListingAdapter(): ListingAdapter{
        val loadMoreScrollListener = LoadMoreScrollListener(
            binding.list.layoutManager as GridLayoutManager
        ) { model.loadAfter() }

        val adapter = ListingAdapter(
            postActions = postActions,
            messageActions = messageActions,
            subredditActions = subredditActions,
            retry = model::retry,
            onLastItemReached = loadMoreScrollListener::lastItemReached)

        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })

        model.initialListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.loadInitial(it)
                model.loadInitialFinished()
            }
        })

        binding.list.addOnScrollListener(loadMoreScrollListener)

        model.additionalListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.loadMore(it)
                model.loadAfterFinished()
                loadMoreScrollListener.finishedLoading()
            }
        })
        (binding.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        if(arguments?.getSerializable(SUBREDDIT_WHERE_KEY) != null){
            model.subscribedSubs.observe(viewLifecycleOwner, Observer {
                if(it != null){
                    adapter.setSubscribedSubs(it.asDomainModel())
                }
            })
        }

        return adapter
    }

    private fun createTrendingAdapter(): TrendingAdapter{
        val loadMoreScrollListener = LoadMoreScrollListener(
            binding.list.layoutManager as GridLayoutManager
        ) {model.loadAfter()}

        val adapter = TrendingAdapter(subredditActions!!, {model.retry()}, {loadMoreScrollListener.lastItemReached()})
        binding.list.adapter = adapter

        model.subscribedSubs.observe(viewLifecycleOwner, Observer {
            if(it != null)
                adapter.setSubscribedSubs(it.asDomainModel())
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })

        model.initialListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.loadInitial(it.toTrendingPosts())
                model.loadInitialFinished()
            }
        })

        binding.list.addOnScrollListener(loadMoreScrollListener)

        model.additionalListing.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.loadMore(it.toTrendingPosts())
                model.loadAfterFinished()
                loadMoreScrollListener.finishedLoading()
            }
        })
        (binding.list.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        return adapter
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

    companion object{
        fun newInstance(profileInfo: ProfileInfo, postSort: PostSort, time: Time?, pageSize: Int): SimpleListingScrollerFragment{
            val fragment = SimpleListingScrollerFragment()
            val args = bundleOf(PROFILE_INFO_KEY to profileInfo, POST_SORT_KEY to postSort, TIME_KEY to time, PAGE_SIZE_KEY to pageSize)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(subreddit: Subreddit, postSort: PostSort, time: Time?, pageSize: Int, useTrendingAdapter: Boolean = false): SimpleListingScrollerFragment {
            val fragment = SimpleListingScrollerFragment()
            val args = bundleOf(SUBREDDIT_KEY to subreddit, POST_SORT_KEY to postSort, TIME_KEY to time, PAGE_SIZE_KEY to pageSize, USE_TRENDING_ADAPTER_KEY to useTrendingAdapter)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(messageWhere: MessageWhere, pageSize: Int): SimpleListingScrollerFragment {
            val fragment = SimpleListingScrollerFragment()
            val args = bundleOf(MESSAGE_WHERE_KEY to messageWhere, PAGE_SIZE_KEY to pageSize)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(subredditWhere: SubredditWhere, pageSize: Int): SimpleListingScrollerFragment{
            val fragment = SimpleListingScrollerFragment()
            val args = bundleOf(SUBREDDIT_WHERE_KEY to subredditWhere, PAGE_SIZE_KEY to pageSize)
            fragment.arguments = args
            return fragment
        }

    }
}