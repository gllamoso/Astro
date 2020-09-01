package dev.gtcl.astro.ui.fragments.search

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
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.SubredditActions
import dev.gtcl.astro.databinding.FragmentSearchBinding
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.ListingScrollListener
import dev.gtcl.astro.ui.ListingAdapter
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.AccountPage
import dev.gtcl.astro.ui.fragments.ListingPage


class SearchFragment : Fragment(), ItemClickListener, SubredditActions {
    private lateinit var binding: FragmentSearchBinding

    private val model: SearchVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(SearchVM::class.java)
    }

    private val args: SearchFragmentArgs by navArgs()

    private val activityModel: MainActivityVM by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSearchBinding.inflate(inflater)
        binding.model = model
        binding.lifecycleOwner = viewLifecycleOwner

        if(!model.firstPageLoaded){
            model.loadPopular()
        }

        initEditTextListener()
        initPopularItems()
        initSearchRecyclerViewAdapter()
        initOnClickListeners()

        if(args.multiSelectMode){
            setMultiSelect()
        } else {
            setPostSearch()
        }

        if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            showKeyboard()
        }

        model.errorMessage.observe(viewLifecycleOwner, {
            if(it != null){
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                model.errorMessageObserved()
            }
        })

        return binding.root
    }

    private fun initEditTextListener(){
        binding.fragmentSearchSearchText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            private val handler = Handler(Looper.getMainLooper())
            private var workRunnable: Runnable? = null
            private val DELAY = 300L

            override fun afterTextChanged(s: Editable?) {
                workRunnable?.let { handler.removeCallbacks(it) }
                workRunnable = Runnable {
                    if(s.isNullOrBlank()){
                        model.showPopular(true)
                    } else {
                        model.searchSubreddits(s.toString())
                        model.showPopular(false)
                    }
                }
                handler.postDelayed(workRunnable!!, DELAY)
            }

        })
    }

    private fun initPopularItems(){
        val recycler = binding.fragmentSearchPopularList
        val scrollListener = ListingScrollListener(15, binding.fragmentSearchPopularList.layoutManager as GridLayoutManager, model::loadMorePopular)
        val listAdapter = ListingAdapter(markwon = null, subredditActions = this, itemClickListener = this, username = null){
            recycler.apply {
                removeOnScrollListener(scrollListener)
                addOnScrollListener(scrollListener)
                model.retry()
            }
        }
        recycler.apply {
            adapter = listAdapter
            addOnScrollListener(scrollListener)
        }

        model.popularItems.observe(viewLifecycleOwner, {
            listAdapter.submitList(it)
            scrollListener.finishedLoading()
        })

        model.morePopularItems.observe(viewLifecycleOwner, {
            if(it != null){
                scrollListener.finishedLoading()
                listAdapter.addItems(it)
                model.morePopularItemsObserved()
            }
        })

        model.networkState.observe(viewLifecycleOwner, {
            listAdapter.networkState = it
        })

        model.lastItemReached.observe(viewLifecycleOwner, {
            if(it == true){
                recycler.removeOnScrollListener(scrollListener)
            }
        })
    }

    private fun initSearchRecyclerViewAdapter(){
        val adapter = SearchAdapter(this, this)
        val searchList = binding.fragmentSearchSearchList
        searchList.adapter = adapter

        model.searchItems.observe(viewLifecycleOwner, {
            adapter.submitList(it)
            searchList.smoothScrollToPosition(0)
        })
    }

    private fun initOnClickListeners(){
        binding.fragmentSearchToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
            hideKeyboard()
        }
    }

    private fun setMultiSelect(){

        val adapter = SelectedItemsAdapter{
            model.removeSelectedItem(it)
        }

        binding.fragmentSearchSearchText.imeOptions = EditorInfo.IME_ACTION_DONE

        binding.fragmentSearchSelectedItems.adapter = adapter

        model.selectedItems.observe(viewLifecycleOwner, {
            adapter.submitList(ArrayList(it))
        })

        binding.fragmentSearchFab.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(SELECTED_SUBREDDITS_KEY, model.selectedItems.value?.toList() ?: listOf())
            navController.popBackStack()
        }
    }

    private fun setPostSearch(){
        binding.fragmentSearchFab.visibility = View.GONE
        binding.fragmentSearchSearchText.apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { textView, _, _ ->
                val query = textView.text.toString()
                findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(ListingPage(SearchListing(query))))
                hideKeyboard()
                true
            }
        }
        binding.fragmentSearchSearchButton.setOnClickListener {
            val query = binding.fragmentSearchSearchText.text.toString()
            findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(ListingPage(SearchListing(query))))
            hideKeyboard()
        }
    }

    override fun itemClicked(item: Item, position: Int) {
        val multiSelectMode = args.multiSelectMode
        if(multiSelectMode){
            val name = when (item) {
                is Subreddit ->  item.displayName
                is Account ->  item.subreddit.displayName
                else ->  throw IllegalStateException("Invalid account: $item")
            }
            model.addSelectedItem(name)
        } else {
            if(item is Account){
                findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(AccountPage(item.name)))
            } else {
                findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(ListingPage(
                    SubredditListing((item as Subreddit).displayName)
                )))
            }
            hideKeyboard()
        }
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        activityModel.subscribe(subreddit, subscribe)
    }

    private fun showKeyboard(){
        binding.fragmentSearchSearchText.requestFocus()
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