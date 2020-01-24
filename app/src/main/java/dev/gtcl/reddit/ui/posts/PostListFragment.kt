package dev.gtcl.reddit.ui.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.databinding.FragmentPostListBinding
import dev.gtcl.reddit.network.NetworkState
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.posts.RedditPost
import dev.gtcl.reddit.posts.asReadPost
import dev.gtcl.reddit.ui.MainActivity
import dev.gtcl.reddit.ui.MainActivityViewModel
import dev.gtcl.reddit.ui.posts.sort_sheet.SortSheetDialogFragment
import dev.gtcl.reddit.ui.posts.time_period_sheet.TimePeriodSheetDialogFragment

class PostListFragment : Fragment() {

    companion object {
        const val KEY_SUBREDDIT = "subreddit"
        const val DEFAULT_SUBREDDIT = "funny"
    }

    private var binding: FragmentPostListBinding? = null
    private lateinit var adapter: PostListAdapter

    private val parentViewModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }

    private val viewModel: PostListViewModel by lazy {
        val viewModelFactory = PostListViewModelFactory(activity!!.application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(PostListViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(binding == null)
            setupFragment(inflater)
        return binding!!.root
    }

    private fun setupFragment(inflater: LayoutInflater){
        binding = FragmentPostListBinding.inflate(inflater)
        binding!!.lifecycleOwner = this
        binding!!.viewModel = parentViewModel

        initAdapter()
        initSwipeToRefresh()
//        initSearch()
//        val subredditSelected = savedInstanceState?.getString(KEY_SUBREDDIT) ?: DEFAULT_SUBREDDIT
//        model.getPosts(Subreddit(displayName = subredditSelected))

        parentViewModel.subredditSelected.observe(this, Observer {
            (activity as AppCompatActivity).supportActionBar?.title = it.displayName
        })

        //TODO: Delete
        binding!!.sortButton.setOnClickListener{
            SortSheetDialogFragment(parentViewModel.sortSelected.value!!) { sort ->
                // TODO: Move logic in ViewModel
                if (sort == PostSort.TOP || sort == PostSort.CONTROVERSIAL) {
                    TimePeriodSheetDialogFragment { time ->
                        if (parentViewModel.getPosts(parentViewModel.subredditSelected.value, sort, time)) {
                            binding!!.list.scrollToPosition(0)
                            (binding!!.list.adapter as? PostListAdapter)?.submitList(null)
                        }
                    }.show(fragmentManager!!, "test2")
                } else {
                    if (parentViewModel.getPosts(parentViewModel.subredditSelected.value, sort)) {
                        binding!!.list.scrollToPosition(0)
                        (binding!!.list.adapter as? PostListAdapter)?.submitList(null)
                    }
                }

            }.show(fragmentManager!!, "test")
        }

        binding!!.subredditButton.setOnClickListener {
            viewModel.displaySubredditSelector()
        }

        viewModel.navigateToSubredditSelection.observe(this, Observer {
            if(it != null){
                this.findNavController().navigate(PostListFragmentDirections.actionPostListFragmentToSubredditSelectorFragment())
                viewModel.displaySubredditSelectorComplete()
            }
        })

        // viewModel.selectPost(it)
        viewModel.navigateToPostDetails.observe(this, Observer {
            if(it != null){
                this.findNavController().navigate(PostListFragmentDirections.actionPostListFragmentToPostDetailsFragment(it))
                viewModel.postSelectionCompleted()
            }
        })

        parentViewModel.subredditSelected.observe(this, Observer {
            binding!!.title.text = it.displayName
        })

        binding!!.toolbar.setNavigationOnClickListener {
            if(activity is MainActivity)
                (activity as MainActivity).showDrawer()
        }
    }

    override fun onResume() {
        super.onResume()
        parentViewModel.renewAccessToken()
    }

    private val postClickListener = object : PostClickListener{
        override fun onPostClick(redditPost: RedditPost?, position: Int) {
            redditPost?.let {
                viewModel.addReadPost(redditPost.asReadPost())
                viewModel.selectPost(it)
            }
        }

    }

    private fun initAdapter() {
        adapter = PostListAdapter({parentViewModel.retry()}, postClickListener)

        binding!!.list.adapter = adapter
        parentViewModel.networkState.observe(this, Observer {
            adapter.setNetworkState(it)
        })

        viewModel.allReadPosts.observe(this, Observer {
            adapter.setReadSubs(it)
        })
    }

    private fun initSwipeToRefresh() {
        parentViewModel.refreshState.observe(this, Observer {
            binding!!.swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })
        binding!!.swipeRefresh.setOnRefreshListener {
            parentViewModel.refresh()
        }
    }

    //    private fun initSearch(){
//        binding.input.setOnEditorActionListener { _, actionId, _ ->
//            if(actionId == EditorInfo.IME_ACTION_GO){
//                updatedSubredditFromInput()
//                true
//            } else {
//                false
//            }
//        }
//
//        binding.input.setOnKeyListener { _, keyCode, event ->
//            if(event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER){
//                updatedSubredditFromInput()
//                true
//            } else {
//                false
//            }
//
//        }
//    }
//
//    private fun updatedSubredditFromInput(){
//        binding.input.text.trim().toString().let {
////            if(it.isNotEmpty()){
////                if(model.showSubreddit(it)){
////                if(model.getPosts(it, sort, t)){
////                if(model.getPosts(it)){
////                    binding.list.scrollToPosition(0)
////                    (binding.list.adapter as? PostListAdapter)?.submitList(null)
////                }
//                if(model.getPosts(it)){
//                    binding.list.scrollToPosition(0)
//                    (binding.list.adapter as? PostListAdapter)?.submitList(null)
//                }
////            }
//        }
//    }
}
