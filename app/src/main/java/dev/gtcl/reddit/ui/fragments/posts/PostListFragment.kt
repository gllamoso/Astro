package dev.gtcl.reddit.ui.fragments.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.databinding.FragmentPostListBinding
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.posts.RedditPost
import dev.gtcl.reddit.posts.asReadPost
import dev.gtcl.reddit.ui.fragments.MainFragment
import dev.gtcl.reddit.ui.fragments.MainFragmentViewModel
import dev.gtcl.reddit.ui.fragments.posts.sort_sheet.SortSheetDialogFragment
import dev.gtcl.reddit.ui.fragments.posts.subreddits.SubredditSelectorDialogFragment
import dev.gtcl.reddit.ui.fragments.posts.time_period_sheet.TimePeriodSheetDialogFragment

class PostListFragment : Fragment() {

    private lateinit var binding: FragmentPostListBinding
    private lateinit var adapter: PostListAdapter

    private val parentViewModel: MainFragmentViewModel by lazy {
        (parentFragment as MainFragment).model
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(!::binding.isInitialized)
            setupFragment(inflater)
        return binding.root
    }

    private fun setupFragment(inflater: LayoutInflater){
        binding = FragmentPostListBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.viewModel = parentViewModel

        initAdapter()
        initSwipeToRefresh()
//        val subredditSelected = savedInstanceState?.getString(KEY_SUBREDDIT) ?: DEFAULT_SUBREDDIT
//        model.getPosts(Subreddit(displayName = subredditSelected))

        //TODO: Delete
        binding.sortButton.setOnClickListener{
            SortSheetDialogFragment(parentViewModel.sortSelected.value!!) { sort ->
                // TODO: Move logic in ViewModel
                if (sort == PostSort.TOP || sort == PostSort.CONTROVERSIAL) {
                    TimePeriodSheetDialogFragment { time ->
                        if (parentViewModel.getPosts(parentViewModel.subredditSelected.value, sort, time)) {
                            binding.list.scrollToPosition(0)
                            (binding.list.adapter as? PostListAdapter)?.submitList(null)
                        }
                    }.show(parentFragmentManager, "test2")
                } else {
                    if (parentViewModel.getPosts(parentViewModel.subredditSelected.value, sort)) {
                        binding.list.scrollToPosition(0)
                        (binding.list.adapter as? PostListAdapter)?.submitList(null)
                    }
                }

            }.show(parentFragmentManager, "test")
        }


        // TODO: Edit
//        val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
//        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//        binding.subredditButton.setOnClickListener {
//            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
//        }
        binding.subredditButton.setOnClickListener{
            SubredditSelectorDialogFragment().show(parentFragmentManager, "test3")
        }

    }

    override fun onResume() {
        super.onResume()
//        parentViewModel.renewAccessToken()
    }

    private val postClickListener = object : PostClickListener {
        override fun onPostClick(redditPost: RedditPost?, position: Int) {
            redditPost?.let {
                parentViewModel.addReadPost(redditPost.asReadPost())
                parentViewModel.selectPost(it)
                parentViewModel.scrollToPage(1)
                parentViewModel.setPost(it)
                parentViewModel.getPostAndComments()
            }
        }

    }

    private fun initAdapter() {
        adapter = PostListAdapter({parentViewModel.retry()}, postClickListener)

        binding.list.adapter = adapter
        parentViewModel.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })

        parentViewModel.allReadPosts.observe(viewLifecycleOwner, Observer {
            adapter.setReadSubs(it)
        })
    }

    private fun initSwipeToRefresh() {
        parentViewModel.refreshState.observe(viewLifecycleOwner, Observer {
            binding.swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })
        binding.swipeRefresh.setOnRefreshListener {
            parentViewModel.refresh()
        }
    }

}
