package dev.gtcl.reddit.ui.fragments.dialog.subreddits

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.R
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.SubscribeAction
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.databinding.FragmentDialogSubredditsBinding
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditListing
import dev.gtcl.reddit.actions.ListingActions
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.mine.MineFragment
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.popular.PopularFragment
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.search.SearchFragment
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.trending.TrendingFragment
import kotlin.NoSuchElementException

class SubredditSelectorDialogFragment: BottomSheetDialogFragment(), SubredditActions {

    private lateinit var binding: FragmentDialogSubredditsBinding

    val model: SubredditSelectorViewModel by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SubredditSelectorViewModel::class.java)
    }

    lateinit var listingActions: ListingActions

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is MineFragment -> childFragment.setFragment(listingActions, this)
            is TrendingFragment -> childFragment.setFragment(this)
            is PopularFragment -> childFragment.setFragment(this)
            is SearchFragment -> childFragment.setFragment(this)
        }
    }

    override fun onStart() {
        super.onStart()

        if(dialog != null){
            val bottomSheet = dialog!!.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        view?.post {
            val parent = requireView().parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior
            val bottomSheetBehavior = behavior as BottomSheetBehavior
            bottomSheetBehavior.peekHeight = (0.75 * requireView().measuredHeight).toInt()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.navdrawer_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDialogSubredditsBinding.inflate(inflater)
        binding.refreshButton.setOnClickListener {
            for(fragment: Fragment in childFragmentManager.fragments){
                when(fragment){
                    is MineFragment -> fragment.syncSubscribedSubs()
                }
            }
        }
        setupTabLayout()
        setEditTextListener()
        return binding.root
    }

    private fun setupTabLayout(){
        val tabLayout = binding.tabLayout
        val viewPager = binding.viewPager
        val adapter = SubredditStateAdapter(this)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager){ tab, position ->
            tab.text = getText(when(position){
                0 -> R.string.mine_tab_label
                1 -> R.string.trending_tab_label
                2 -> R.string.popular_tab_label
                3 -> R.string.search_tab_label
                else -> throw NoSuchElementException("No such tab in the following position: $position")
            })
        }.attach()
        viewPager.offscreenPageLimit = 3
    }

    @Suppress("UNREACHABLE_CODE")
    private fun setEditTextListener(){
        binding.searchText.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            private val handler = Handler(Looper.getMainLooper())
            private var workRunnable: Runnable? = null
            private val DELAY = 100L

            override fun afterTextChanged(s: Editable?) {
                workRunnable?.let { handler.removeCallbacks(it) }
                workRunnable = Runnable {
                    if(!s.isNullOrBlank()){
                        binding.viewPager.currentItem = 3
                        searchSubreddit(s.toString())
                    }
                }
               handler.postDelayed(workRunnable!!, DELAY)
            }

        })

        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
    }

    override fun onClick(subreddit: Subreddit) {
        listingActions.onListingClicked(SubredditListing(subreddit))
    }

    override fun addToFavorites(subreddit: Subreddit, favorite: Boolean, refresh: Boolean) {
        model.addToFavorites(subreddit, favorite)
        if(refresh) refreshMineFragment()
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean, refresh: Boolean) {
        model.subscribe(subreddit, if(subscribe) SubscribeAction.SUBSCRIBE else SubscribeAction.UNSUBSCRIBE, false)
        if(refresh) refreshMineFragment()
    }

    private fun refreshMineFragment(){
        for(fragment: Fragment in childFragmentManager.fragments){
            if(fragment is MineFragment){
                fragment.refresh()
                return
            }
        }
    }

    private fun searchSubreddit(query: String){
        for(fragment: Fragment in childFragmentManager.fragments){
            if(fragment is SearchFragment){
                fragment.searchSubreddit(query)
                return
            }
        }
    }

    companion object {
        val TAG = SubredditSelectorDialogFragment::class.qualifiedName
    }

}