package dev.gtcl.reddit.ui.fragments.item_scroller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.*
import dev.gtcl.reddit.databinding.FragmentItemScrollerBinding
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.ItemScrollListener
import dev.gtcl.reddit.ui.ListingItemAdapter

open class ItemScrollerFragment : Fragment(), PostActions, MessageActions, SubredditActions, ItemClickListener{

    private lateinit var binding: FragmentItemScrollerBinding
    private lateinit var scrollListener: ItemScrollListener
    private lateinit var listAdapter: ListingItemAdapter

    private var parentItemClickListener: ItemClickListener? = null
    private var parentPostActions: PostActions? = null
    private var parentSubredditActions: SubredditActions? = null
    private var parentMessageActions: MessageActions? = null

    val model: ItemScrollerVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(ItemScrollerVM::class.java)
    }

    fun setActions(listener: ItemClickListener, postActions: PostActions? = null, subredditActions: SubredditActions? = null, messageActions: MessageActions? = null){
        parentItemClickListener = listener
        parentPostActions = postActions
        parentSubredditActions = subredditActions
        parentMessageActions = messageActions
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)
        listAdapter = ListingItemAdapter(this, this, this, this, model::retry, false)
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
        parentPostActions?.vote(post, vote)
    }

    override fun share(post: Post) {
        parentPostActions?.share(post)
    }

    override fun viewProfile(post: Post) {
        parentPostActions?.viewProfile(post)
    }

    override fun save(post: Post) {
        parentPostActions?.save(post)
    }

    override fun hide(post: Post) {
        parentPostActions?.hide(post)
    }

    override fun report(post: Post) {
        parentPostActions?.report(post)
    }

    override fun thumbnailClicked(post: Post) {
        model.addReadItem(post)
        parentPostActions?.thumbnailClicked(post)
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
        parentMessageActions?.reply(message)
    }

    override fun mark(message: Message) {
        parentMessageActions?.mark(message)
    }

    override fun delete(message: Message) {
        parentMessageActions?.delete(message)
    }

    override fun viewProfile(user: String) {
        parentMessageActions?.viewProfile(user)
    }

    override fun block(user: String) {
        parentMessageActions?.block(user)
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

//     _____ _                    _____ _ _      _      _      _     _
//    |_   _| |                  / ____| (_)    | |    | |    (_)   | |
//      | | | |_ ___ _ __ ___   | |    | |_  ___| | __ | |     _ ___| |_ ___ _ __   ___ _ __
//      | | | __/ _ \ '_ ` _ \  | |    | | |/ __| |/ / | |    | / __| __/ _ \ '_ \ / _ \ '__|
//     _| |_| ||  __/ | | | | | | |____| | | (__|   <  | |____| \__ \ ||  __/ | | |  __/ |
//    |_____|\__\___|_| |_| |_|  \_____|_|_|\___|_|\_\ |______|_|___/\__\___|_| |_|\___|_|

    override fun itemClicked(item: Item) {
        model.addReadItem(item)
        parentItemClickListener?.itemClicked(item)
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