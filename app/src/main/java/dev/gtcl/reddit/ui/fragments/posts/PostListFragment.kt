package dev.gtcl.reddit.ui.fragments.posts

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.R
import dev.gtcl.reddit.STATE
import dev.gtcl.reddit.database.asDomainModel
import dev.gtcl.reddit.databinding.FragmentPostListBinding
import dev.gtcl.reddit.databinding.NavHeaderBinding
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.posts.RedditPost
import dev.gtcl.reddit.ui.*
import dev.gtcl.reddit.ui.fragments.ImageVideoViewerDialogFragment
import dev.gtcl.reddit.ui.fragments.MainFragment
import dev.gtcl.reddit.ui.fragments.MainFragmentViewModel
import dev.gtcl.reddit.ui.fragments.posts.sort_sheet.SortSheetDialogFragment
import dev.gtcl.reddit.ui.fragments.posts.subreddits.SubredditSelectorDialogFragment
import dev.gtcl.reddit.ui.fragments.posts.time_period_sheet.TimePeriodSheetDialogFragment
import dev.gtcl.reddit.ui.webview.WebviewActivity
import dev.gtcl.reddit.users.User

class PostListFragment : Fragment() {

    private lateinit var binding: FragmentPostListBinding

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }
    private val model: MainFragmentViewModel by lazy {
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
        binding.viewModel = model

        //TODO: Add
//        val subredditSelected = savedInstanceState?.getString(KEY_SUBREDDIT) ?: DEFAULT_SUBREDDIT
//        model.getPosts(Subreddit(displayName = subredditSelected))

        setRecyclerView()
        setSwipeToRefresh()
        setDrawer(inflater)
        setBottomAppbarClickListeners()
    }

    private val postClickListener = object : PostClickListener {
        override fun onPostClicked(redditPost: RedditPost?, position: Int) {
            redditPost?.let {
                model.addReadPost(redditPost.asReadPost())
                model.setPost(it)
                model.scrollToPage(1)
                model.setPost(it)
                model.fetchPostAndComments()
            }
        }

        override fun onThumbnailClicked(post: RedditPost) {
            val dialogFragment = ImageVideoViewerDialogFragment()
            dialogFragment.setPost(post)
            dialogFragment.show(parentFragmentManager, "test")
        }

    }

    private fun setRecyclerView() {
        val adapter = PostListAdapter({model.retry()}, postClickListener)

        binding.list.adapter = adapter
        model.networkState.observe(viewLifecycleOwner, Observer {
            adapter.setNetworkState(it)
        })

        model.allReadPosts.observe(viewLifecycleOwner, Observer {
            adapter.setReadSubs(it)
        })
    }

    private fun setSwipeToRefresh() {
        model.refreshState.observe(viewLifecycleOwner, Observer {
            binding.swipeRefresh.isRefreshing = it == NetworkState.LOADING
        })
        binding.swipeRefresh.setOnRefreshListener {
            model.refresh()
        }
    }

    @SuppressLint("WrongConstant")
    private fun setDrawer(inflater: LayoutInflater){
        val drawerLayout = binding.drawerLayout
        val header = NavHeaderBinding.inflate(inflater)

        binding.expandableListView.addHeaderView(header.root)

        val adapter = CustomExpandableListAdapter(context!!, object: AdapterOnClickListeners{
            override fun onAddAccountClicked() { signInUser() }

            override fun onRemoveAccountClicked(username: String) { parentModel.deleteUserFromDatabase(username) }

            override fun onAccountClicked(user: User) {
                parentModel.setCurrentUser(user, true)
                drawerLayout.closeDrawers()
            }

            override fun onLogoutClicked() { parentModel.setCurrentUser(null, true) }

        })

        binding.expandableListView.setAdapter(adapter)

        model.allUsers.observe(viewLifecycleOwner, Observer {
            adapter.setUsers(it.asDomainModel())
        })

        parentModel.currentUser.observe(viewLifecycleOwner, Observer {
            header.user = it
        })

        binding.toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(Gravity.START)
        }
    }

    private fun setBottomAppbarClickListeners(){
        //TODO: Delete
        binding.sortButton.setOnClickListener{
            SortSheetDialogFragment(model.sortSelected.value!!) { sort ->
                // TODO: Move logic in ViewModel
                if (sort == PostSort.TOP || sort == PostSort.CONTROVERSIAL) {
                    TimePeriodSheetDialogFragment { time ->
                        if (model.fetchPosts(model.subredditSelected.value, sort, time)) {
                            binding.list.scrollToPosition(0)
                            (binding.list.adapter as? PostListAdapter)?.submitList(null)
                        }
                    }.show(parentFragmentManager, "test2")
                } else {
                    if (model.fetchPosts(model.subredditSelected.value, sort)) {
                        binding.list.scrollToPosition(0)
                        (binding.list.adapter as? PostListAdapter)?.submitList(null)
                    }
                }

            }.show(parentFragmentManager, "test")
        }


        // TODO: Edit
        binding.subredditButton.setOnClickListener{
            model.fetchDefaultSubreddits()
            SubredditSelectorDialogFragment().show(parentFragmentManager, "test3")
        }

        binding.refreshButton.setOnClickListener{
            model.refresh()
        }
    }

    private fun signInUser() {
        val url = String.format(getString(R.string.auth_url), getString(R.string.client_id), STATE, getString(R.string.redirect_uri))
        val intent = Intent(context, WebviewActivity::class.java)
        intent.putExtra(URL_KEY, url)
        activity?.startActivityForResult(intent, REDIRECT_URL_REQUEST_CODE)
    }
}
