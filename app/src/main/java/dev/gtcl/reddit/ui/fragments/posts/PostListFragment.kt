package dev.gtcl.reddit.ui.fragments.posts

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dev.gtcl.reddit.PostSort
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.STATE
import dev.gtcl.reddit.database.asDomainModel
import dev.gtcl.reddit.databinding.FragmentPostListBinding
import dev.gtcl.reddit.databinding.NavHeaderBinding
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.posts.Post
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.ui.*
import dev.gtcl.reddit.ui.fragments.ImageVideoViewerDialogFragment
import dev.gtcl.reddit.ui.fragments.posts.sort_sheet.SortSheetDialogFragment
import dev.gtcl.reddit.ui.fragments.subreddits.SubredditOnClickListener
import dev.gtcl.reddit.ui.fragments.subreddits.SubredditSelectorDialogFragment
import dev.gtcl.reddit.ui.fragments.posts.time_period_sheet.TimePeriodSheetDialogFragment
import dev.gtcl.reddit.ui.webview.WebviewActivity
import dev.gtcl.reddit.users.User

class PostListFragment : Fragment() {

    private lateinit var binding: FragmentPostListBinding

    private val parentModel: MainActivityViewModel by lazy {
        (activity as MainActivity).model
    }
    val model: PostListViewModel by lazy {
        val viewModelFactory = PostListViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(PostListViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if(!::binding.isInitialized)
            setupFragment(inflater)
        return binding.root
    }

    private fun setupFragment(inflater: LayoutInflater){
        binding = FragmentPostListBinding.inflate(inflater)
        binding.lifecycleOwner = this
        binding.model = model

        //TODO: Add
//        val subredditSelected = savedInstanceState?.getString(KEY_SUBREDDIT) ?: DEFAULT_SUBREDDIT
//        model.getPosts(Subreddit(displayName = subredditSelected))

        // TODO: Update. Wrap with observer, observing a refresh live data
        parentModel.fetchData.observe(viewLifecycleOwner, Observer{
            if(it) { model.fetchPosts(Subreddit(displayName = "funny")) }
        })

        setRecyclerView()
        setSwipeToRefresh()
        setDrawer(inflater)
        setBottomAppbarClickListeners()
    }

    private lateinit var postClickListener: (Post) -> (Unit)

    fun setPostSelectionListener(postClickListener: (Post) -> (Unit)){
        this.postClickListener = postClickListener
    }

    private val postViewClickListener = object : PostViewClickListener {
        override fun onPostClicked(post: Post?, position: Int) {
            post?.let {
                model.addReadPost(it.asReadPost())
                postClickListener(it)
            }
        }

        override fun onThumbnailClicked(post: Post) {
            val dialogFragment = ImageVideoViewerDialogFragment()
            dialogFragment.setPost(post)
            dialogFragment.show(parentFragmentManager, "test")
        }

    }

    private fun setRecyclerView() {
        val adapter = PostListAdapter({model.retry()}, postViewClickListener)

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

        val adapter =
            CustomExpandableListAdapter(
                requireContext(),
                object :
                    AdapterOnClickListeners {
                    override fun onAddAccountClicked() {
                        signInUser()
                    }

                    override fun onRemoveAccountClicked(username: String) {
                        parentModel.deleteUserFromDatabase(username)
                    }

                    override fun onAccountClicked(user: User) {
                        parentModel.setCurrentUser(user, true)
                    }

                    override fun onLogoutClicked() {
                        parentModel.setCurrentUser(null, true)
                    }

                })

        binding.expandableListView.setAdapter(adapter)

        parentModel.allUsers.observe(viewLifecycleOwner, Observer {
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
                // TODO: Move logic in ViewModel?
                if (sort == PostSort.TOP || sort == PostSort.CONTROVERSIAL) {
                    TimePeriodSheetDialogFragment { time ->
                        model.fetchPosts(model.subredditSelected.value!!, sort, time)
                        binding.list.scrollToPosition(0)
                        (binding.list.adapter as? PostListAdapter)?.submitList(null)
                    }.show(parentFragmentManager, TimePeriodSheetDialogFragment.TAG)
                } else {
                    model.fetchPosts(model.subredditSelected.value!!, sort)
                    binding.list.scrollToPosition(0)
                    (binding.list.adapter as? PostListAdapter)?.submitList(null)
                }

            }.show(parentFragmentManager, SortSheetDialogFragment.TAG)
        }


        binding.subredditButton.setOnClickListener{
            val subredditSelector = SubredditSelectorDialogFragment()
            subredditSelector.setSubredditOnClickListener(object : SubredditOnClickListener {
                override fun onClick(sub: Subreddit) {
                    model.fetchPosts(sub)
                    subredditSelector.dismiss()
                }
            })
            subredditSelector.show(parentFragmentManager, SubredditSelectorDialogFragment.TAG)
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
