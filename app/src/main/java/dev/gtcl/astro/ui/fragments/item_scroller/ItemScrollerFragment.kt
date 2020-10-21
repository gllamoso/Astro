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
import dev.gtcl.astro.ui.fragments.manage.ManagePostDialogFragment
import dev.gtcl.astro.ui.fragments.media.MediaDialogFragment
import dev.gtcl.astro.ui.fragments.share.ShareCommentOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.share.SharePostOptionsDialogFragment
import dev.gtcl.astro.ui.fragments.reply_or_edit.ReplyOrEditDialogFragment
import dev.gtcl.astro.ui.fragments.report.ReportDialogFragment
import dev.gtcl.astro.ui.fragments.subreddits.SubredditInfoDialogFragment
import dev.gtcl.astro.ui.fragments.view_pager.*
import io.noties.markwon.Markwon

open class ItemScrollerFragment : Fragment(), PostActions, CommentActions, MessageActions,
    SubredditActions, ItemClickListener, LinkHandler {

    private var binding: FragmentItemScrollerBinding? = null

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

    private val sharedPref by lazy {
        (requireActivity().application as AstroApplication).sharedPref
    }

    override fun onResume() {
        super.onResume()
        val scrollPosition =
            (binding?.fragmentItemScrollerList?.layoutManager as GridLayoutManager?)?.findFirstCompletelyVisibleItemPosition()
        if (scrollPosition == 0) { // Fix for recyclerview items not being updated
            binding?.fragmentItemScrollerList?.scrollToPosition(0)
        }
        val showNsfw = sharedPref.getBoolean(NSFW_KEY, false)
        val blurNsfwThumbnail = sharedPref.getBoolean(NSFW_THUMBNAIL_KEY, false)
        if (showNsfw != model.showNsfw) {
            binding?.fragmentItemScrollerSwipeRefresh?.isRefreshing = true
            listAdapter.blurNsfw = blurNsfwThumbnail
            initData()
        } else if (blurNsfwThumbnail != listAdapter.blurNsfw) {
            listAdapter.blurNsfw = blurNsfwThumbnail
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentItemScrollerBinding.inflate(inflater)
        binding?.model = model
        binding?.lifecycleOwner = viewLifecycleOwner

        if (!model.initialPageLoaded) {
            initData()
        }

        initScroller()
        initOtherObservers()

        return binding?.root
    }

    private fun resetOnScrollListener() {
        binding?.fragmentItemScrollerList?.apply {
            clearOnScrollListeners()
            addOnScrollListener(scrollListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun initData() {
        initListingInfo()
        model.fetchFirstPage()
    }

    private fun initListingInfo() {
        val showNsfw = sharedPref.getBoolean(NSFW_KEY, false)
        model.setNsfw(showNsfw)
        val args = requireArguments()
        when {
            args.getSerializable(LISTING_KEY) != null -> {
                val listing = args.getSerializable(LISTING_KEY) as PostListing
                val postSort = args.getSerializable(POST_SORT_KEY) as PostSort
                val time = args.getSerializable(TIME_KEY) as Time?
                model.setListingInfo(listing, true)
                model.setListingSort(postSort, time)
            }
            args.getSerializable(MESSAGE_WHERE_KEY) != null -> {
                val messageWhere = args.getSerializable(MESSAGE_WHERE_KEY) as MessageWhere
                model.setListingInfo(messageWhere)
            }
            args.getSerializable(SUBREDDIT_WHERE_KEY) != null -> {
                val subredditWhere = args.getSerializable(SUBREDDIT_WHERE_KEY) as SubredditWhere
                model.setListingInfo(subredditWhere)
            }
            else -> throw IllegalStateException("Missing key arguments")
        }
    }

    private fun initScroller() {
        val recyclerView = binding?.fragmentItemScrollerList
        val swipeRefresh = binding?.fragmentItemScrollerSwipeRefresh
        scrollListener = ListingScrollListener(
            15,
            recyclerView?.layoutManager as GridLayoutManager,
            model::loadMore
        )
        val blurNsfw = sharedPref.getBoolean(NSFW_THUMBNAIL_KEY, false)
        val currentAccount = (requireActivity().application as AstroApplication).currentAccount
        val inInbox = requireArguments().getSerializable(MESSAGE_WHERE_KEY) != null
        listAdapter = ListingAdapter(
            markwon,
            postActions = this,
            commentActions = this,
            messageActions = this,
            expected = if (inInbox) ItemType.Message else ItemType.Post,
            blurNsfw = blurNsfw,
            itemClickListener = this,
            username = currentAccount?.name
        ) {
            resetOnScrollListener()
            model.retry()
        }
        recyclerView.apply {
            this.adapter = listAdapter
            addOnScrollListener(scrollListener)
        }

        model.items.observe(viewLifecycleOwner, {
            scrollListener.finishedLoading()
            listAdapter.submitList(it)
            binding?.fragmentItemScrollerList?.scrollToPosition(0)
        })

        model.moreItems.observe(viewLifecycleOwner, {
            if (it != null) {
                scrollListener.finishedLoading()
                listAdapter.addItems(it)
                model.moreItemsObserved()
            }
        })

        model.networkState.observe(viewLifecycleOwner, {
            listAdapter.networkState = it
            if (it == NetworkState.LOADED || it.status == Status.FAILED) {
                swipeRefresh?.isRefreshing = false
            }
        })

        model.lastItemReached.observe(viewLifecycleOwner, {
            if (it == true) {
                recyclerView.clearOnScrollListeners()
            }
        })

        swipeRefresh?.setOnRefreshListener {
            if (model.networkState.value == NetworkState.LOADING) {
                swipeRefresh.isRefreshing = false
                return@setOnRefreshListener
            }

            resetOnScrollListener()
            initData()
        }
    }

    private fun initOtherObservers() {
        model.errorMessage.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                binding?.root?.let {
                    Snackbar.make(it, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                model.errorMessageObserved()
            }
        })

        childFragmentManager.setFragmentResultListener(URL_KEY, viewLifecycleOwner, { _, bundle ->
            handleLink(bundle.getString(URL_KEY) ?: "")
        })

        childFragmentManager.setFragmentResultListener(
            REPORT_KEY,
            viewLifecycleOwner,
            { _, bundle ->
                val position = bundle.getInt(POSITION_KEY, -1)
                if (position != -1) {
                    model.removeItemAt(position)
                    listAdapter.removeAt(position)
                }
            })

        childFragmentManager.setFragmentResultListener(MANAGE_POST_KEY, viewLifecycleOwner,
            { _, bundle ->
                val position = bundle.getInt(POSITION_KEY)
                val nsfw = bundle.getBoolean(NSFW_KEY)
                val spoiler = bundle.getBoolean(SPOILER_KEY)
                val getNotification = bundle.getBoolean(GET_NOTIFICATIONS_KEY)
                val flair = bundle.get(FLAIRS_KEY) as Flair?
                val post = (model.items.value ?: return@setFragmentResultListener)[position] as Post
                activityModel.updatePost(post, nsfw, spoiler, getNotification, flair)
                listAdapter.notifyItemChanged(position)
            })

        childFragmentManager.setFragmentResultListener(NEW_REPLY_KEY, viewLifecycleOwner,
            { _, bundle ->
                val item = bundle.get(ITEM_KEY) as Item
                val position = bundle.getInt(POSITION_KEY)
                val reply = bundle.getBoolean(NEW_REPLY_KEY)
                if (!reply && position != -1) {
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
        checkIfLoggedInBeforeExecuting(requireContext()) {
            post.updateScore(vote)
            activityModel.vote(post.name, vote)
        }
    }

    override fun share(post: Post) {
        SharePostOptionsDialogFragment.newInstance(post).show(parentFragmentManager, null)
    }

    override fun viewProfile(post: Post) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(post.author)
            )
        )
    }

    override fun save(post: Post) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            post.saved = !post.saved
            activityModel.save(post.name, post.saved)
        }
    }

    override fun subredditSelected(sub: String) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                ListingPage(SubredditListing(sub))
            )
        )
    }

    override fun hide(post: Post, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.hide(post.name, post.hidden)
            if (post.hidden) {
                model.removeItemAt(position)
                listAdapter.removeAt(position)
            }
        }
    }

    override fun report(post: Post, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ReportDialogFragment.newInstance(post, position).show(childFragmentManager, null)
        }
    }

    override fun thumbnailClicked(post: Post, position: Int) {
        model.addReadItem(post)
        when (val urlType: UrlType? = post.urlFormatted?.getUrlType()) {
            UrlType.OTHER -> activityModel.openChromeTab(post.urlFormatted)
            UrlType.REDDIT_GALLERY -> {
                val galleryItems = post.galleryAsMediaItems
                if (galleryItems == null) {
                    context?.let { thisContext ->
                        Toast.makeText(
                            thisContext,
                            getString(R.string.media_failed),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
                val dialog = MediaDialogFragment.newInstance(
                    post.urlFormatted,
                    galleryItems ?: return,
                    PostPage(post, position)
                )
                dialog.show(parentFragmentManager, null)
            }
            null -> itemClicked(post, position)
            else -> {
                val mediaType = when (urlType) {
                    UrlType.IMGUR_ALBUM -> MediaType.IMGUR_ALBUM
                    UrlType.IMGUR_IMAGE -> MediaType.IMGUR_PICTURE
                    UrlType.GIF -> MediaType.GIF
                    UrlType.GFYCAT -> MediaType.GFYCAT
                    UrlType.REDGIFS -> MediaType.REDGIFS
                    UrlType.IMAGE -> MediaType.PICTURE
                    UrlType.HLS, UrlType.GIFV, UrlType.STANDARD_VIDEO, UrlType.REDDIT_VIDEO -> MediaType.VIDEO
                    else -> null
                }
                if (mediaType == null) {
                    handleLink(post.urlFormatted)
                    return
                }
                val url = when (mediaType) {
                    MediaType.VIDEO -> {
                        if (urlType == UrlType.GIFV) {
                            post.urlFormatted.replace(".gifv", ".mp4")
                        } else {
                            post.previewVideoUrl ?: return
                        }
                    }
                    else -> post.urlFormatted.formatHtmlEntities()
                }
                val backupUrl = when (mediaType) {
                    MediaType.GFYCAT, MediaType.REDGIFS -> post.previewVideoUrl
                    MediaType.VIDEO -> {
                        if (urlType == UrlType.GIFV) {
                            post.previewVideoUrl
                        } else {
                            null
                        }
                    }
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
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ReplyOrEditDialogFragment.newInstance(post, position, false)
                .show(childFragmentManager, null)
        }
    }

    override fun manage(post: Post, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ManagePostDialogFragment.newInstance(post, position).show(childFragmentManager, null)
        }
    }

    override fun delete(post: Post, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
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
        checkIfLoggedInBeforeExecuting(requireContext()) {
            comment.updateScore(vote)
            activityModel.vote(comment.name, vote)
        }
    }

    override fun save(comment: Comment) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            comment.saved = comment.saved != true
            activityModel.save(comment.name, comment.saved == true)
        }
    }

    override fun share(comment: Comment) {
        ShareCommentOptionsDialogFragment.newInstance(comment).show(parentFragmentManager, null)
    }

    override fun reply(comment: Comment, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            if (comment.locked == true || comment.deleted) {
                Toast.makeText(
                    requireContext(),
                    R.string.cannot_reply_to_comment,
                    Toast.LENGTH_LONG
                ).show()
            } else {
                ReplyOrEditDialogFragment.newInstance(comment, position, true)
                    .show(childFragmentManager, null)
            }
        }
    }

    override fun mark(comment: Comment) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            comment.new = !(comment.new ?: false)
            activityModel.markMessage(comment, !(comment.new ?: false))
        }
    }

    override fun block(comment: Comment, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.block(comment)
            listAdapter.removeAt(position)
            model.removeItemAt(position)
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
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ReportDialogFragment.newInstance(comment, position).show(childFragmentManager, null)
        }
    }

    override fun edit(comment: Comment, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ReplyOrEditDialogFragment.newInstance(comment, position, false)
                .show(childFragmentManager, null)
        }
    }

    override fun delete(comment: Comment, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
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
        checkIfLoggedInBeforeExecuting(requireContext()) {
            ReplyOrEditDialogFragment.newInstance(message, -1, true)
                .show(childFragmentManager, null)
        }
    }

    override fun mark(message: Message) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            message.new = !message.new
            activityModel.markMessage(message, !message.new)
        }
    }

    override fun delete(message: Message, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
            activityModel.deleteMessage(message)
            listAdapter.removeAt(position)
            model.removeItemAt(position)
        }
    }

    override fun viewProfile(message: Message) {
        findNavController().navigate(
            ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                AccountPage(message.author)
            )
        )
    }

    override fun block(message: Message, position: Int) {
        checkIfLoggedInBeforeExecuting(requireContext()) {
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

    override fun viewMoreInfo(displayName: String) {
        if (displayName.startsWith("u_")) {
            findNavController().navigate(
                ViewPagerFragmentDirections.actionViewPagerFragmentSelf(
                    AccountPage(displayName.removePrefix("u_"))
                )
            )
        } else {
            SubredditInfoDialogFragment.newInstance(displayName).show(childFragmentManager, null)
        }
    }

//     _____ _                    _____ _ _      _      _      _     _
//    |_   _| |                  / ____| (_)    | |    | |    (_)   | |
//      | | | |_ ___ _ __ ___   | |    | |_  ___| | __ | |     _ ___| |_ ___ _ __   ___ _ __
//      | | | __/ _ \ '_ ` _ \  | |    | | |/ __| |/ / | |    | / __| __/ _ \ '_ \ / _ \ '__|
//     _| |_| ||  __/ | | | | | | |____| | | (__|   <  | |____| \__ \ ||  __/ | | |  __/ |
//    |_____|\__\___|_| |_| |_|  \_____|_|_|\___|_|\_\ |______|_|___/\__\___|_| |_|\___|_|

    override fun itemClicked(item: Item, position: Int) {
        when (item) {
            is Post -> {
                model.addReadItem(item)
                activityModel.newViewPagerPage(PostPage(item, position))
            }
            is Message -> {
                ReplyOrEditDialogFragment.newInstance(item, position, true)
                    .show(childFragmentManager, null)
            }
            is Comment -> {
                if (item.permalinkFormatted != null) {
                    val permalink = "https://www.reddit.com${item.permalinkFormatted}"
                    activityModel.newViewPagerPage(CommentsPage(permalink, true))
                } else {
                    val context = item.contextFormatted
                    if (context.isNullOrBlank()) {
                        throw Exception("Comment has no permalink or context: $item")
                    }
                    val regex = "/[a-z0-9]+/\\?context=[0-9]+".toRegex()
                    val link = if (item.parentId?.startsWith("t1_") == true) {
                        context.replace(regex, item.parentId.replace("t1_", "/"))
                    } else {
                        context
                    }
                    activityModel.newViewPagerPage(
                        CommentsPage(
                            "https://www.reddit.com$link",
                            true
                        )
                    )
                }
            }
        }
    }

    override fun handleLink(link: String) {
        activityModel.handleLink(link)
    }

//     _   _                 _____           _
//    | \ | |               |_   _|         | |
//    |  \| | _____      __   | |  _ __  ___| |_ __ _ _ __   ___ ___
//    | . ` |/ _ \ \ /\ / /   | | | '_ \/ __| __/ _` | '_ \ / __/ _ \
//    | |\  |  __/\ V  V /   _| |_| | | \__ \ || (_| | | | | (_|  __/
//    |_| \_|\___| \_/\_/   |_____|_| |_|___/\__\__,_|_| |_|\___\___|
//

    companion object {
        fun newInstance(
            postListing: PostListing,
            postSort: PostSort,
            time: Time?
        ): ItemScrollerFragment {
            return ItemScrollerFragment().apply {
                arguments = bundleOf(
                    LISTING_KEY to postListing,
                    POST_SORT_KEY to postSort,
                    TIME_KEY to time
                )
            }
        }

        fun newInstance(messageWhere: MessageWhere): ItemScrollerFragment {
            val fragment =
                ItemScrollerFragment()
            val args = bundleOf(MESSAGE_WHERE_KEY to messageWhere)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(subredditWhere: SubredditWhere): ItemScrollerFragment {
            val fragment =
                ItemScrollerFragment()
            val args = bundleOf(SUBREDDIT_WHERE_KEY to subredditWhere)
            fragment.arguments = args
            return fragment
        }

    }

}