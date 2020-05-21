package dev.gtcl.reddit.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.MessageActions
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.ListingItemAdapter
import dev.gtcl.reddit.ui.fragments.dialog.ShareOptionsDialogFragment
import dev.gtcl.reddit.ui.fragments.media.MediaDialogFragment

open class ListingScrollerFragment : Fragment(), PostActions, MessageActions, SubredditActions, ItemClickListener{

    private lateinit var binding: FragmentItemScrollerBinding

    val model: ListingScrollerViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ListingScrollerViewModel::class.java)
    }

    private val listAdapter: ListingItemAdapter by lazy{
        ListingItemAdapter(this,
            this,
            this,
            this,
            model::retry,
            false)
    }

    private val scrollChangeListener by lazy{
        NestedScrollListener(loadMore = model::loadAfter)
    }

    private var parentItemClickListener: ItemClickListener? = null

    fun setActions(listener: ItemClickListener){
        parentItemClickListener = listener
    }

    private fun setListingInfo(){
        val args = requireArguments()
        when{
            args.getSerializable(PROFILE_INFO_KEY) != null -> {
                val profileInfo = args.getSerializable(PROFILE_INFO_KEY) as ProfileInfo
                val postSort = args.getSerializable(POST_SORT_KEY) as PostSort
                val time = args.getSerializable(TIME_KEY) as Time?
                val pageSize = args.getInt(PAGE_SIZE_KEY)
                val user = args.getString(USER_KEY)
                model.setListingInfo(ProfileListing(profileInfo), postSort, time, pageSize)
                model.user = user
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

    override fun onResume() {
        super.onResume()
        if(requireArguments().getSerializable(SUBREDDIT_WHERE_KEY) != null) {
            model.syncWithDb()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)
        binding.nestedScrollView.setOnScrollChangeListener(scrollChangeListener)
        binding.list.adapter = listAdapter
        setSwipeRefresh()
        setObservers()
        if(!model.initialPageLoaded){
            setListingInfo()
            model.loadInitialDataAndFirstPage()
        }
        return binding.root
    }

    private fun setObservers(){
        model.items.observe(viewLifecycleOwner, Observer {
            if(it != null){
                listAdapter.clearItems()
                listAdapter.addItems(it)
                scrollChangeListener.finishedLoading()
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
                scrollChangeListener.finishedLoading()
            }
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
            binding.progressBar.visibility = if(it == NetworkState.LOADING) View.VISIBLE else View.GONE
            listAdapter.networkState = it
        })

        if(requireArguments().getSerializable(SUBREDDIT_WHERE_KEY) != null){
            model.favoriteSubs.observe(viewLifecycleOwner, Observer {
                if(it != null) {
                    listAdapter.updateFavoriteItems(it)
                    model.favoriteSubsSynced()
                }
            })
            model.subscribedSubs.observe(viewLifecycleOwner, Observer {
                if(it != null){
                    listAdapter.updateSubscribedItems(it)
                    model.subredditsSynced()
                }
            })
        }
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

//     _____          _                  _   _
//    |  __ \        | |       /\       | | (_)
//    | |__) |__  ___| |_     /  \   ___| |_ _  ___  _ __  ___
//    |  ___/ _ \/ __| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | (_) \__ \ |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_|   \___/|___/\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun vote(post: Post, vote: Vote) {
        model.vote(post.name, vote)
    }

    override fun share(post: Post) {
        ShareOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
    }

    override fun viewProfile(post: Post) {
        val bundle = bundleOf(USER_KEY to post.author)
        findNavController().navigate(R.id.account_fragment, bundle)
    }

    override fun save(post: Post) {
        if(post.saved) model.unsave(post.name)
        else model.save(post.name)
    }

    override fun hide(post: Post) {
        if(!post.hidden) model.hide(post.name)
        else model.unhide(post.name)
    }

    override fun report(post: Post) {
        TODO("Not yet implemented")
    }

    override fun thumbnailClicked(post: Post) {
        model.addReadItem(post)
        Log.d("TAE","Post clicked: $post")
        val urlType = when {
            post.isImage -> UrlType.IMAGE
            post.isGif -> UrlType.GIF
            post.isGfycat -> UrlType.GFYCAT
            post.isGfv -> UrlType.GIFV
            post.isRedditVideo -> UrlType.M3U8
            else -> UrlType.LINK
        }
        Log.d("TAE", "UrlType: $urlType")
        val dialog = MediaDialogFragment.newInstance(
            if(urlType == UrlType.M3U8 || urlType == UrlType.GIFV) post.videoUrl!! else post.url!!,
            urlType,
            post)
        dialog.show(childFragmentManager, null)
    }

//     __  __                                               _   _
//    |  \/  |                                    /\       | | (_)
//    | \  / | ___  ___ ___  __ _  __ _  ___     /  \   ___| |_ _  ___  _ __  ___
//    | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | |  __/\__ \__ \ (_| | (_| |  __/  / ____ \ (__| |_| | (_) | | | \__ \
//    |_|  |_|\___||___/___/\__,_|\__, |\___| /_/    \_\___|\__|_|\___/|_| |_|___/
//                                 __/ |
//                                |___/

    override fun reply(message: Message) {
        TODO("Not yet implemented")
    }

    override fun mark(message: Message) {
        TODO("Not yet implemented")
    }

    override fun delete(message: Message) {
        TODO("Not yet implemented")
    }

    override fun viewProfile(user: String) {
        val bundle = bundleOf(USER_KEY to user)
        findNavController().navigate(R.id.account_fragment, bundle)
    }

    override fun block(user: String) {
        TODO("Not yet implemented")
    }

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun favorite(subreddit: Subreddit, favorite: Boolean) {
        model.addToFavorites(subreddit, favorite)
//        if(refresh) refreshMineFragment()
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        model.subscribe(subreddit, if(subscribe) SubscribeAction.SUBSCRIBE else SubscribeAction.UNSUBSCRIBE, false)
//        if(refresh) refreshMineFragment()
    }

//     _____ _                    _____ _ _      _      _      _     _
//    |_   _| |                  / ____| (_)    | |    | |    (_)   | |
//      | | | |_ ___ _ __ ___   | |    | |_  ___| | __ | |     _ ___| |_ ___ _ __   ___ _ __
//      | | | __/ _ \ '_ ` _ \  | |    | | |/ __| |/ / | |    | / __| __/ _ \ '_ \ / _ \ '__|
//     _| |_| ||  __/ | | | | | | |____| | | (__|   <  | |____| \__ \ ||  __/ | | |  __/ |
//    |_____|\__\___|_| |_| |_|  \_____|_|_|\___|_|\_\ |______|_|___/\__\___|_| |_|\___|_|

    override fun itemClicked(item: Item) {
        parentItemClickListener?.itemClicked(item)
        model.addReadItem(item)
    }

//     _   _                 _____           _
//    | \ | |               |_   _|         | |
//    |  \| | _____      __   | |  _ __  ___| |_ __ _ _ __   ___ ___
//    | . ` |/ _ \ \ /\ / /   | | | '_ \/ __| __/ _` | '_ \ / __/ _ \
//    | |\  |  __/\ V  V /   _| |_| | | \__ \ || (_| | | | | (_|  __/
//    |_| \_|\___| \_/\_/   |_____|_| |_|___/\__\__,_|_| |_|\___\___|
//

    companion object{
        fun newInstance(profileInfo: ProfileInfo, postSort: PostSort, time: Time?, pageSize: Int, user: String? = null): ListingScrollerFragment{
            val fragment = ListingScrollerFragment()
            val args = bundleOf(PROFILE_INFO_KEY to profileInfo, POST_SORT_KEY to postSort, TIME_KEY to time, PAGE_SIZE_KEY to pageSize, USER_KEY to user)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(subreddit: Subreddit, postSort: PostSort, time: Time?, pageSize: Int, useTrendingAdapter: Boolean = false): ListingScrollerFragment {
            val fragment = ListingScrollerFragment()
            val args = bundleOf(SUBREDDIT_KEY to subreddit, POST_SORT_KEY to postSort, TIME_KEY to time, PAGE_SIZE_KEY to pageSize, USE_TRENDING_ADAPTER_KEY to useTrendingAdapter)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(messageWhere: MessageWhere, pageSize: Int): ListingScrollerFragment {
            val fragment = ListingScrollerFragment()
            val args = bundleOf(MESSAGE_WHERE_KEY to messageWhere, PAGE_SIZE_KEY to pageSize)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(subredditWhere: SubredditWhere, pageSize: Int): ListingScrollerFragment{
            val fragment = ListingScrollerFragment()
            val args = bundleOf(SUBREDDIT_WHERE_KEY to subredditWhere, PAGE_SIZE_KEY to pageSize)
            fragment.arguments = args
            return fragment
        }

    }

}