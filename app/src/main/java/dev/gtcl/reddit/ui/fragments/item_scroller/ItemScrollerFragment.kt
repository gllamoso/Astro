package dev.gtcl.reddit.ui.fragments.item_scroller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.*
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding
import dev.gtcl.reddit.models.reddit.listing.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.ItemScrollListener
import dev.gtcl.reddit.ui.ListingItemAdapter
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.AccountPage
import dev.gtcl.reddit.ui.fragments.ViewPagerFragmentDirections
import dev.gtcl.reddit.ui.fragments.media.MediaDialogFragment
import dev.gtcl.reddit.ui.fragments.misc.ShareOptionsDialogFragment

open class ItemScrollerFragment : Fragment(), PostActions, MessageActions, SubredditActions, ItemClickListener{

    private lateinit var binding: FragmentItemScrollerBinding
    private lateinit var scrollListener: ItemScrollListener
    private lateinit var listAdapter: ListingItemAdapter

    private var viewPagerActions: ViewPagerActions? = null
    private var navigationActions: NavigationActions? = null

    val model: ItemScrollerVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ItemScrollerVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    fun setActions(viewPagerActions: ViewPagerActions?, navigationActions: NavigationActions?){
        this.viewPagerActions = viewPagerActions
        this.navigationActions = navigationActions
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
                model.setListingInfo(
                    ProfileListing(
                        profileInfo
                    ), postSort, time, pageSize)
                model.user = user
            }
            args.getParcelable<Subreddit>(SUBREDDIT_KEY) != null -> {
                val subreddit = args.getParcelable<Subreddit>(SUBREDDIT_KEY)!!
                val postSort = args.getSerializable(POST_SORT_KEY) as PostSort
                val time = args.getSerializable(TIME_KEY) as Time?
                val pageSize = args.getInt(PAGE_SIZE_KEY)
                model.setListingInfo(
                    SubredditListing(
                        subreddit
                    ), postSort, time, pageSize)
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
        binding = FragmentItemScrollerBinding.inflate(inflater)
        listAdapter = ListingItemAdapter(this, this, this, this, model::retry)
        scrollListener = ItemScrollListener(15, binding.list.layoutManager as GridLayoutManager, model::loadItems)
        binding.list.adapter = listAdapter
        binding.list.addOnScrollListener(scrollListener)
        setSwipeRefresh()
        setObservers()
        if(!model.initialPageLoaded){
            setListingInfo()
            model.loadItems()
        }

        return binding.root
    }

    private fun setObservers(){
        model.items.observe(viewLifecycleOwner, Observer {
            listAdapter.setItems(it)
            scrollListener.finishedLoading()
            if(it.isEmpty()){
                binding.list.visibility = View.GONE
                binding.noResultsText.visibility = View.VISIBLE
            } else {
                binding.list.visibility = View.VISIBLE
                binding.noResultsText.visibility = View.GONE
            }
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
            listAdapter.networkState = it
        })

        model.lastItemReached.observe(viewLifecycleOwner, Observer {
            if(it == true){
                binding.list.removeOnScrollListener(scrollListener)
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

//     _____          _                  _   _
//    |  __ \        | |       /\       | | (_)
//    | |__) |__  ___| |_     /  \   ___| |_ _  ___  _ __  ___
//    |  ___/ _ \/ __| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | (_) \__ \ |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_|   \___/|___/\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun vote(post: Post, vote: Vote) {
        activityModel.vote(post.name, vote)
    }

    override fun share(post: Post) {
        ShareOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
    }

    override fun viewProfile(post: Post) {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(post.author)))
    }

    override fun save(post: Post) {
        activityModel.save(post.name, post.saved)
    }

    override fun hide(post: Post, position: Int) {
        activityModel.hide(post.name, post.hidden)
        if(post.hidden){
            model.removeItemAt(position)
        }
    }

    override fun report(post: Post) {
        TODO("Implement reporting")
    }

    override fun thumbnailClicked(post: Post) {
        model.addReadItem(post)
        val urlType = when {
            post.isImage -> UrlType.IMAGE
            post.isGif -> UrlType.GIF
            post.isGfycat -> UrlType.GFYCAT
            post.isGfv -> UrlType.GIFV
            post.isRedditVideo -> UrlType.M3U8
            else -> UrlType.LINK
        }
        if(urlType == UrlType.LINK){
            navigationActions?.launchWebview(post.url!!)
        } else {
            val dialog = MediaDialogFragment.newInstance(
                if(urlType == UrlType.M3U8 || urlType == UrlType.GIFV) post.videoUrl!! else post.url!!,
                urlType,
                post)
            dialog.show(childFragmentManager, null)
        }
    }

//     __  __                                               _   _
//    |  \/  |                                    /\       | | (_)
//    | \  / | ___  ___ ___  __ _  __ _  ___     /  \   ___| |_ _  ___  _ __  ___
//    | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |  | |  __/\__ \__ \ (_| | (_| |  __/  / ____ \ (__| |_| | (_) | | | \__ \
//    |_|  |_|\___||___/___/\__,_|\__, |\___| /_/    \_\___|\__|_|\___/|_| |_|___/
//                                 __/ |
//                                |___/

    override fun reply(message: Message) {}

    override fun mark(message: Message) {}

    override fun delete(message: Message) {}

    override fun viewProfile(user: String) {
        navigationActions?.accountSelected(user)
    }

    override fun block(user: String) {}

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        activityModel.subscribe(subreddit, subscribe)
    }

//     _____ _                    _____ _ _      _      _      _     _
//    |_   _| |                  / ____| (_)    | |    | |    (_)   | |
//      | | | |_ ___ _ __ ___   | |    | |_  ___| | __ | |     _ ___| |_ ___ _ __   ___ _ __
//      | | | __/ _ \ '_ ` _ \  | |    | | |/ __| |/ / | |    | / __| __/ _ \ '_ \ / _ \ '__|
//     _| |_| ||  __/ | | | | | | |____| | | (__|   <  | |____| \__ \ ||  __/ | | |  __/ |
//    |_____|\__\___|_| |_| |_|  \_____|_|_|\___|_|\_\ |______|_|___/\__\___|_| |_|\___|_|

    override fun itemClicked(item: Item) {
        model.addReadItem(item)
        viewPagerActions?.navigateToNewPage(item)
    }

//     _   _                 _____           _
//    | \ | |               |_   _|         | |
//    |  \| | _____      __   | |  _ __  ___| |_ __ _ _ __   ___ ___
//    | . ` |/ _ \ \ /\ / /   | | | '_ \/ __| __/ _` | '_ \ / __/ _ \
//    | |\  |  __/\ V  V /   _| |_| | | \__ \ || (_| | | | | (_|  __/
//    |_| \_|\___| \_/\_/   |_____|_| |_|___/\__\__,_|_| |_|\___\___|
//

    companion object{
        fun newInstance(profileInfo: ProfileInfo, postSort: PostSort, time: Time?, pageSize: Int, user: String? = null): ItemScrollerFragment {
            val fragment =
                ItemScrollerFragment()
            val args = bundleOf(PROFILE_INFO_KEY to profileInfo, POST_SORT_KEY to postSort, TIME_KEY to time, PAGE_SIZE_KEY to pageSize, USER_KEY to user)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(subreddit: Subreddit, postSort: PostSort, time: Time?, pageSize: Int, useTrendingAdapter: Boolean = false): ItemScrollerFragment {
            val fragment =
                ItemScrollerFragment()
            val args = bundleOf(SUBREDDIT_KEY to subreddit, POST_SORT_KEY to postSort, TIME_KEY to time, PAGE_SIZE_KEY to pageSize, USE_TRENDING_ADAPTER_KEY to useTrendingAdapter)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(messageWhere: MessageWhere, pageSize: Int): ItemScrollerFragment {
            val fragment =
                ItemScrollerFragment()
            val args = bundleOf(MESSAGE_WHERE_KEY to messageWhere, PAGE_SIZE_KEY to pageSize)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(subredditWhere: SubredditWhere, pageSize: Int): ItemScrollerFragment {
            val fragment =
                ItemScrollerFragment()
            val args = bundleOf(SUBREDDIT_WHERE_KEY to subredditWhere, PAGE_SIZE_KEY to pageSize)
            fragment.arguments = args
            return fragment
        }

    }

}