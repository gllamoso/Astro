package dev.gtcl.astro.ui.fragments.item_scroller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.*
import dev.gtcl.astro.databinding.FragmentItemScrollerBinding
import dev.gtcl.astro.models.reddit.MediaURL
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.network.NetworkState
import dev.gtcl.astro.network.Status
import dev.gtcl.astro.ui.ListingScrollListener
import dev.gtcl.astro.ui.ListingAdapter
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.*
import dev.gtcl.astro.ui.fragments.manage.ManagePostDialogFragment
import dev.gtcl.astro.ui.fragments.media.MediaDialogFragment
import dev.gtcl.astro.ui.fragments.share.ShareCommentOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.share.SharePostOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.reply_or_edit.ReplyOrEditDialogFragment
import dev.gtcl.astro.ui.fragments.report.ReportDialogFragment
import io.noties.markwon.Markwon

open class ItemScrollerFragment : Fragment(), PostActions, CommentActions, MessageActions, SubredditActions, ItemClickListener, LinkHandler{

    private lateinit var binding: FragmentItemScrollerBinding

    private lateinit var scrollListener: ListingScrollListener
    private lateinit var listAdapter: ListingAdapter

    val model: ItemScrollerVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(ItemScrollerVM::class.java)
    }

    private val markwon: Markwon by lazy {
        createMarkwonInstance(requireContext(), ::handleLink)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    override fun onResume() {
        super.onResume()
        val scrollPosition = (binding.fragmentItemScrollerList.layoutManager as GridLayoutManager?)?.findFirstCompletelyVisibleItemPosition()
        if(scrollPosition == 0){ // Fix for recyclerview items not being updated
            binding.fragmentItemScrollerList.scrollToPosition(0)
        }
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity().application as AstroApplication)
        val showNsfw = sharedPref.getBoolean(NSFW_KEY, false)
        val blurNsfwThumbnail = sharedPref.getBoolean(NSFW_THUMBNAIL_KEY, false)
        if(showNsfw != model.showNsfw){
            binding.fragmentItemScrollerSwipeRefresh.isRefreshing = true
            listAdapter.blurNsfw = blurNsfwThumbnail
            initData()
        } else if(blurNsfwThumbnail != listAdapter.blurNsfw){
            listAdapter.blurNsfw = blurNsfwThumbnail
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = viewLifecycleOwner

        if(!model.initialPageLoaded){
            initData()
        }

        initScroller()
        initOtherObservers()

        return binding.root
    }

    private fun initData(){
        initListingInfo()
        model.fetchFirstPage()
    }

    private fun initListingInfo(){
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(requireActivity().application as AstroApplication)
        val showNsfw = sharedPref.getBoolean(NSFW_KEY, false)
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
                    ), postSort, time, pageSize, showNsfw)
                model.user = user
            }
            args.getSerializable(SUBREDDIT_KEY) != null -> {
                val subreddit = args.getString(SUBREDDIT_KEY)!!
                val postSort = args.getSerializable(POST_SORT_KEY) as PostSort
                val time = args.getSerializable(TIME_KEY) as Time?
                val pageSize = args.getInt(PAGE_SIZE_KEY)
                model.setListingInfo(
                    SubredditListing(
                        subreddit
                    ), postSort, time, pageSize, showNsfw)
            }
            args.getSerializable(MESSAGE_WHERE_KEY) != null -> {
                val messageWhere = args.getSerializable(MESSAGE_WHERE_KEY) as MessageWhere
                val pageSize = args.getInt(PAGE_SIZE_KEY)
                model.setListingInfo(messageWhere, pageSize, showNsfw)
            }
            args.getSerializable(SUBREDDIT_WHERE_KEY) != null -> {
                val subredditWhere = args.getSerializable(SUBREDDIT_WHERE_KEY) as SubredditWhere
                val pageSize = args.getInt(PAGE_SIZE_KEY)
                model.setListingInfo(subredditWhere, pageSize, showNsfw)
            }
            else -> throw IllegalStateException("Missing key arguments")
        }
    }

    private fun initScroller(){
        val recyclerView = binding.fragmentItemScrollerList
        val swipeRefresh = binding.fragmentItemScrollerSwipeRefresh
        scrollListener = ListingScrollListener(15, recyclerView.layoutManager as GridLayoutManager, model::loadMore)
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val blurNsfw = preferences.getBoolean(NSFW_THUMBNAIL_KEY, false)
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        listAdapter = ListingAdapter(
            markwon,
            postActions = this,
            commentActions = this,
            messageActions = this,
            expected = ItemType.Post,
            blurNsfw = blurNsfw,
            itemClickListener = this,
            username = currentAccount?.name){
            recyclerView.apply {
                removeOnScrollListener(scrollListener)
                addOnScrollListener(scrollListener)
                model.retry()
            }
        }
        recyclerView.apply {
            this.adapter = listAdapter
            addOnScrollListener(scrollListener)
        }

        model.items.observe(viewLifecycleOwner, {
            scrollListener.finishedLoading()
            listAdapter.submitList(it)
            binding.fragmentItemScrollerList.scrollToPosition(0)
        })

        model.moreItems.observe(viewLifecycleOwner, {
            if(it != null){
                scrollListener.finishedLoading()
                listAdapter.addItems(it)
                model.moreItemsObserved()
            }
        })

        model.networkState.observe(viewLifecycleOwner, {
            listAdapter.networkState = it
            if (it == NetworkState.LOADED || it.status == Status.FAILED) {
                swipeRefresh.isRefreshing = false
            }
        })

        model.lastItemReached.observe(viewLifecycleOwner, {
            if (it == true) {
                recyclerView.removeOnScrollListener(scrollListener)
            }
        })

        swipeRefresh.setOnRefreshListener {
            if(model.networkState.value == NetworkState.LOADING){
               swipeRefresh.isRefreshing = false
                return@setOnRefreshListener
            }

            recyclerView.apply {
                removeOnScrollListener(scrollListener)
                addOnScrollListener(scrollListener)
            }
            initData()
        }
    }

    private fun initOtherObservers(){
        model.errorMessage.observe(viewLifecycleOwner, {
            if(it != null){
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        childFragmentManager.setFragmentResultListener(URL_KEY, viewLifecycleOwner, { _, bundle ->
            handleLink(bundle.getString(URL_KEY) ?: "")
        })

        childFragmentManager.setFragmentResultListener(REPORT_KEY, viewLifecycleOwner, { _, bundle ->
            val  position = bundle.getInt(POSITION_KEY, -1)
            if(position != -1){
                model.removeItemAt(position)
                listAdapter.removeAt(position)
            }
        })

        childFragmentManager.setFragmentResultListener(MANAGE_POST_KEY, viewLifecycleOwner,
            { _, bundle ->
                val position = bundle.getInt(POST_KEY)
                val nsfw = bundle.getBoolean(NSFW_KEY)
                val spoiler = bundle.getBoolean(SPOILER_KEY)
                val getNotification = bundle.getBoolean(GET_NOTIFICATIONS_KEY)
                val flair = bundle.get(FLAIRS_KEY) as Flair?
                val post = model.items.value!![position] as Post
                activityModel.updatePost(post, nsfw, spoiler, getNotification, flair)
                listAdapter.notifyItemChanged(position)
            })

        childFragmentManager.setFragmentResultListener(NEW_REPLY_KEY, viewLifecycleOwner,
            { _, bundle ->
                val item = bundle.get(ITEM_KEY) as Item
                val position = bundle.getInt(POSITION_KEY)
                val reply = bundle.getBoolean(NEW_REPLY_KEY)
                if(!reply && position != -1) {
                    model.updateItemAt(position, item)
                    listAdapter.updateAt(position, item)
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
        checkedIfLoggedInBeforeExecuting(requireContext()){
            post.updateScore(vote)
            activityModel.vote(post.name, vote)
        }
    }

    override fun share(post: Post) {
        SharePostOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
    }

    override fun viewProfile(post: Post) {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(AccountPage(post.author)))
    }

    override fun save(post: Post) {
        checkedIfLoggedInBeforeExecuting(requireContext()){
            post.saved = !post.saved
            activityModel.save(post.name, post.saved)
        }
    }

    override fun subredditSelected(sub: String) {
        findNavController().navigate(ViewPagerFragmentDirections.actionViewPagerFragmentSelf(ListingPage(SubredditListing(sub))))
    }

    override fun hide(post: Post, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()){
            activityModel.hide(post.name, post.hidden)
            if(post.hidden){
                model.removeItemAt(position)
                listAdapter.removeAt(position)
            }
        }
    }

    override fun report(post: Post, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()){
            ReportDialogFragment.newInstance(post, position).show(childFragmentManager, null)
        }
    }

    override fun thumbnailClicked(post: Post, position: Int) {
        model.addReadItem(post)
        when (val urlType: UrlType? = post.url?.getUrlType()) {
            UrlType.OTHER -> activityModel.openChromeTab(post.url)
            null -> throw IllegalArgumentException("Post does not have URL")
            else -> {
                val mediaType = when (urlType) {
                    UrlType.IMGUR_ALBUM -> MediaType.IMGUR_ALBUM
                    UrlType.IMGUR_IMAGE -> MediaType.IMGUR_PICTURE
                    UrlType.GIF -> MediaType.GIF
                    UrlType.GFYCAT -> MediaType.GFYCAT
                    UrlType.REDGIFS -> MediaType.REDGIFS
                    UrlType.IMAGE -> MediaType.PICTURE
                    UrlType.HLS, UrlType.GIFV, UrlType.STANDARD_VIDEO, UrlType.REDDIT_VIDEO -> MediaType.VIDEO
                    else -> throw IllegalArgumentException("Invalid media type: $urlType")
                }
                val url = when (mediaType) {
                    MediaType.VIDEO -> post.previewVideoUrl!!
                    else -> post.url
                }
                val backupUrl = when (mediaType) {
                    MediaType.GFYCAT, MediaType.REDGIFS -> post.previewVideoUrl
                    else -> null
                }
                val dialog = MediaDialogFragment.newInstance(
                    MediaURL(url, mediaType, backupUrl),
                    PostPage(post, position)
                )
                dialog.show(parentFragmentManager, null)
            }
        }
    }

    override fun edit(post: Post, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()){
            ReplyOrEditDialogFragment.newInstance(post, position, false).show(childFragmentManager, null)
        }
    }

    override fun manage(post: Post, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()){
            ManagePostDialogFragment.newInstance(post, position).show(childFragmentManager, null)
        }
    }

    override fun delete(post: Post, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()){
            activityModel.delete(post.name)
            model.removeItemAt(position)
            listAdapter.removeAt(position)
        }
    }

//      _____                                     _                  _   _
//     / ____|                                   | |       /\       | | (_)
//    | |     ___  _ __ ___  _ __ ___   ___ _ __ | |_     /  \   ___| |_ _  ___  _ __  ___
//    | |    / _ \| '_ ` _ \| '_ ` _ \ / _ \ '_ \| __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//    | |___| (_) | | | | | | | | | | |  __/ | | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//     \_____\___/|_| |_| |_|_| |_| |_|\___|_| |_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/

    override fun vote(comment: Comment, vote: Vote) {
        checkedIfLoggedInBeforeExecuting(requireContext()){
            comment.updateScore(vote)
            activityModel.vote(comment.name, vote)
        }
    }

    override fun save(comment: Comment) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            comment.saved = comment.saved != true
            activityModel.save(comment.name, comment.saved == true)
        }
    }

    override fun share(comment: Comment) {
        ShareCommentOptionsDialogFragment.newInstance(comment).show(parentFragmentManager, null)
    }

    override fun reply(comment: Comment, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            if(comment.locked == true || comment.deleted){
                Toast.makeText(requireContext(), R.string.cannot_reply_to_comment, Toast.LENGTH_LONG).show()
            } else {
                ReplyOrEditDialogFragment.newInstance(comment, position, true).show(childFragmentManager, null)
            }
        }
    }

    override fun viewProfile(comment: Comment) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(comment.author)
            )
        )
    }

    override fun report(comment: Comment, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            ReportDialogFragment.newInstance(comment, position).show(childFragmentManager, null)
        }
    }

    override fun edit(comment: Comment, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            ReplyOrEditDialogFragment.newInstance(comment, position, false).show(childFragmentManager, null)
        }
    }

    override fun delete(comment: Comment, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.delete(comment.name)
            model.removeItemAt(position)
            listAdapter.removeAt(position)
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

    override fun reply(message: Message) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            ReplyOrEditDialogFragment.newInstance(message, -1, true).show(childFragmentManager, null)
        }
    }

    override fun mark(message: Message, read: Boolean) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            message.new = !read
            activityModel.markMessage(message, read)
        }
    }

    override fun delete(message: Message, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.deleteMessage(message)
            listAdapter.removeAt(position)
            model.removeItemAt(position)
        }
    }

    override fun viewProfile(message: Message) {
        activityModel.newPage(AccountPage(message.author))
    }

    override fun block(message: Message, position: Int) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.block(message)
            listAdapter.removeAt(position)
            model.removeItemAt(position)
        }
    }

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        checkedIfLoggedInBeforeExecuting(requireContext()) {
            subreddit.userSubscribed = subscribe
            activityModel.subscribe(subreddit, subscribe)
        }
    }

//     _____ _                    _____ _ _      _      _      _     _
//    |_   _| |                  / ____| (_)    | |    | |    (_)   | |
//      | | | |_ ___ _ __ ___   | |    | |_  ___| | __ | |     _ ___| |_ ___ _ __   ___ _ __
//      | | | __/ _ \ '_ ` _ \  | |    | | |/ __| |/ / | |    | / __| __/ _ \ '_ \ / _ \ '__|
//     _| |_| ||  __/ | | | | | | |____| | | (__|   <  | |____| \__ \ ||  __/ | | |  __/ |
//    |_____|\__\___|_| |_| |_|  \_____|_|_|\___|_|\_\ |______|_|___/\__\___|_| |_|\___|_|

    override fun itemClicked(item: Item, position: Int) {
        when(item){
            is Post -> {
                model.addReadItem(item)
                activityModel.newPage(PostPage(item, position))
            }
            is Message -> {
                ReplyOrEditDialogFragment.newInstance(item, position, true).show(childFragmentManager, null)
            }
            is Comment -> {
                if(item.permalink != null){
                    val permalink = "https://www.reddit.com${item.permalink}"
                    activityModel.newPage(CommentsPage(permalink, true))
                } else {
                    val context = item.context
                    if(context.isNullOrBlank()){
                        throw Exception("Comment has no permalink or context: $item")
                    }
                    val regex = "/[a-z0-9]+/\\?context=[0-9]+".toRegex()
                    val link = if(item.parentId?.startsWith("t1_") == true){
                        context.replace(regex, item.parentId.replace("t1_", "/"))
                    } else {
                        context
                    }
                    activityModel.newPage(CommentsPage("https://www.reddit.com$link", true))
                }
            }
        }
    }

    override fun handleLink(link: String) {
        parentFragmentManager.setFragmentResult(URL_KEY, bundleOf(URL_KEY to link))
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

        fun newInstance(subreddit: String, postSort: PostSort, time: Time?, pageSize: Int, useTrendingAdapter: Boolean = false): ItemScrollerFragment {
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