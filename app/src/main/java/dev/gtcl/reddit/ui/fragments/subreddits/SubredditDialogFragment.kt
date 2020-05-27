package dev.gtcl.reddit.ui.fragments.subreddits

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.databinding.FragmentDialogSubredditsBinding
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.actions.ListingTypeClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.ListingType
import dev.gtcl.reddit.models.reddit.SubredditListing
import dev.gtcl.reddit.ui.fragments.item_scroller.ItemScrollerFragment
import dev.gtcl.reddit.ui.fragments.subreddits.mine.MineFragment
import dev.gtcl.reddit.ui.fragments.subreddits.search.SearchFragment
import dev.gtcl.reddit.ui.fragments.subreddits.trending.TrendingListFragment
import kotlin.NoSuchElementException

class SubredditDialogFragment: BottomSheetDialogFragment(), SubredditActions, ListingTypeClickListener, ItemClickListener {

    private lateinit var binding: FragmentDialogSubredditsBinding

    val model: SubredditSelectorVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SubredditSelectorVM::class.java)
    }

    private var parentListingTypeClickListener: ListingTypeClickListener? = null
    fun setActions(listingTypeClickListener: ListingTypeClickListener){
        parentListingTypeClickListener = listingTypeClickListener
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when(childFragment){
            is MineFragment -> childFragment.setActions(this, this)
            is TrendingListFragment -> childFragment.setActions(this, this)
            is ItemScrollerFragment -> childFragment.setActions(this, subredditActions = this)
            is SearchFragment -> childFragment.setActions(this, this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.navdrawer_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDialogSubredditsBinding.inflate(inflater)
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

    override fun favorite(subreddit: Subreddit, favorite: Boolean) {
        model.addToFavorites(subreddit, favorite)
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        model.subscribe(subreddit, if(subscribe) SubscribeAction.SUBSCRIBE else SubscribeAction.UNSUBSCRIBE, false)
    }

    private fun searchSubreddit(query: String){
        for(fragment: Fragment in childFragmentManager.fragments){
            if(fragment is SearchFragment){
                fragment.searchSubreddit(query)
                return
            }
        }
    }

    override fun listingTypeClicked(listing: ListingType) {
        parentListingTypeClickListener?.listingTypeClicked(listing)
    }

    override fun itemClicked(item: Item) {
        if(item is Subreddit){
            parentListingTypeClickListener?.listingTypeClicked(SubredditListing(item))
        }
    }

    companion object {
        val TAG = SubredditDialogFragment::class.qualifiedName
    }

}