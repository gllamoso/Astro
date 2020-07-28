package dev.gtcl.reddit.ui.fragments.search

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import dev.gtcl.reddit.*
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.databinding.FragmentSearchBinding
import dev.gtcl.reddit.models.reddit.listing.Account
import dev.gtcl.reddit.models.reddit.listing.Item
import dev.gtcl.reddit.models.reddit.listing.Subreddit
import dev.gtcl.reddit.models.reddit.listing.SubredditListing
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.ItemScrollListener
import dev.gtcl.reddit.ui.ListingItemAdapter
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.AccountPage
import dev.gtcl.reddit.ui.fragments.ListingPage
import io.noties.markwon.Markwon
import kotlinx.android.synthetic.main.fragment_listing.*


class SearchFragment : Fragment(), ItemClickListener, SubredditActions {
    private lateinit var binding: FragmentSearchBinding

    private val model: SearchVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SearchVM::class.java)
    }

    private val args: SearchFragmentArgs by navArgs()

    private val activityModel: MainActivityVM by activityViewModels()

    private val markwon: Markwon by lazy {
        Markwon.builder(requireContext())
            .build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSearchBinding.inflate(inflater)

        if(!model.initialPageLoaded){
            model.loadPopular()
        }

        setEditTextListener()
        setPopularRecyclerViewAdapter()
        setSearchRecyclerViewAdapter()
        setOnClickListeners()

        if(args.multiSelectMode){
            setMultiSelect()
        } else {
            binding.fab.visibility = View.GONE
        }

        if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            showKeyboard()
        }

        return binding.root
    }

    private fun setEditTextListener(){
        binding.searchText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            private val handler = Handler(Looper.getMainLooper())
            private var workRunnable: Runnable? = null
            private val DELAY = 300L

            override fun afterTextChanged(s: Editable?) {
                workRunnable?.let { handler.removeCallbacks(it) }
                workRunnable = Runnable {
                    if(s.isNullOrBlank()){
                        binding.popularList.visibility = View.VISIBLE
                        binding.searchList.visibility = View.GONE
                        binding.noResultsText.visibility = View.GONE
                    } else {
                        model.searchSubreddits(s.toString())
                        binding.popularList.visibility = View.GONE
                        binding.searchList.visibility = View.VISIBLE
                        binding.noResultsText.visibility = View.GONE
                    }
                }
                handler.postDelayed(workRunnable!!, DELAY)
            }

        })
    }

    private fun setPopularRecyclerViewAdapter(){
        val listAdapter = ListingItemAdapter(markwon, subredditActions = this, itemClickListener = this, retry = model::retry)
        val scrollListener = ItemScrollListener(15, binding.popularList.layoutManager as GridLayoutManager, model::loadMorePopular)
        val recycler = binding.popularList
        recycler.apply {
            adapter = listAdapter
            addOnScrollListener(scrollListener)
        }

        model.popularItems.observe(viewLifecycleOwner, Observer {
            listAdapter.submitList(it)
            scrollListener.finishedLoading()
            model.initialPageLoaded = true
        })

        model.morePopularItems.observe(viewLifecycleOwner, Observer {
            if(it != null){
                scrollListener.finishedLoading()
                listAdapter.addItems(it)
                model.morePopularItemsObserved()
            }
        })

        model.lastItemReached.observe(viewLifecycleOwner, Observer {
            if(it == true){
                recycler.removeOnScrollListener(scrollListener)
            }
        })
    }

    private fun setSearchRecyclerViewAdapter(){
        val adapter = SearchAdapter(this, this)
        binding.searchList.adapter = adapter

        model.searchItems.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
            binding.searchList.smoothScrollToPosition(0)
            binding.searchList.visibility = if(it.isEmpty()) View.GONE else View.VISIBLE
            binding.noResultsText.visibility = if(it.isNotEmpty() || binding.searchText.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        })

        model.networkState.observe(viewLifecycleOwner, Observer {
            binding.progressBar.visibility = if(it == NetworkState.LOADED){
                View.GONE
            } else {
                View.VISIBLE
            }
        })
    }

    private fun setOnClickListeners(){
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
            hideKeyboard()
        }
    }

    private fun setMultiSelect(){

        val adapter = SelectedItemsAdapter{
            model.removeSelectedItem(it)
        }

        binding.selectedItemsRecyclerView.adapter = adapter

        model.selectedItems.observe(viewLifecycleOwner, Observer {
            adapter.submitList(ArrayList(it))
        })

        binding.fab.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(SELECTED_SUBREDDITS_KEY, model.selectedItems.value?.toList() ?: listOf())
            navController.popBackStack()
        }
    }

    override fun itemClicked(item: Item, position: Int) {
        val multiSelectMode = args.multiSelectMode
        if(multiSelectMode){
            val name = when (item) {
                is Subreddit ->  item.displayName
                is Account ->  item.subreddit.displayName
                else ->  ""
            }
            model.addSelectedItem(name)
        } else {
            if(item is Account){
                findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(AccountPage(item.name)))
            } else {
                findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(ListingPage(
                    SubredditListing(item as Subreddit)
                )))
            }
            hideKeyboard()
        }
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        activityModel.subscribe(subreddit, subscribe)
    }

    private fun showKeyboard(){
        binding.searchText.requestFocus()
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun hideKeyboard(){
        requireActivity().currentFocus?.let {
            val inputManager: InputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(it.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    companion object{
        fun newInstance(): SearchFragment{
            return SearchFragment()
        }
    }
}