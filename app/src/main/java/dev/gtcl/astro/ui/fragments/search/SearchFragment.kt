package dev.gtcl.astro.ui.fragments.search

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import dev.gtcl.astro.*
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.FragmentSearchBinding
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.ListingScrollListener
import dev.gtcl.astro.ui.ListingAdapter
import dev.gtcl.astro.ui.activities.MainActivityVM
import dev.gtcl.astro.ui.fragments.view_pager.AccountPage
import dev.gtcl.astro.ui.fragments.view_pager.ListingPage


class SearchFragment : Fragment(), ItemClickListener {
    private var binding: FragmentSearchBinding? = null

    private val model: SearchVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as AstroApplication)
        ViewModelProvider(this, viewModelFactory).get(SearchVM::class.java)
    }

    private val args: SearchFragmentArgs by navArgs()

    private val activityModel: MainActivityVM by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(inflater)
        binding?.model = model
        binding?.lifecycleOwner = viewLifecycleOwner

        if (!model.initialPageLoaded) {
            model.fetchFirstPage()
        }

        initEditTextListener()
        initPopularItems()
        initSearchRecyclerViewAdapter()
        initOnClickListeners()

        if (args.multiSelectMode) {
            setMultiSelect()
        } else {
            setPostSearch()
        }

        model.errorMessage.observe(viewLifecycleOwner, { errorMessage ->
            if (errorMessage != null) {
                binding?.root?.let {
                    Snackbar.make(it, errorMessage, Snackbar.LENGTH_LONG).show()
                }
                model.errorMessageObserved()
            }
        })

        activityModel.subredditSelected.observe(viewLifecycleOwner, {
            if (it != null) {
                findNavController().navigate(
                    SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(
                        ListingPage(SubredditListing(it.displayName))
                    )
                )
                activityModel.subredditObserved()
            }
        })

        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding?.fragmentSearchSearchText?.showKeyboard()
            binding?.fragmentSearchSearchText?.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    hideKeyboardFrom(
                        requireContext(),
                        (binding ?: return@setOnFocusChangeListener).fragmentSearchSearchText
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun initEditTextListener() {
        binding?.fragmentSearchSearchText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            private val handler = Handler(Looper.getMainLooper())
            private var workRunnable: Runnable? = null
            private val DELAY = 300L

            override fun afterTextChanged(s: Editable?) {
                workRunnable?.let { handler.removeCallbacks(it) }
                workRunnable = Runnable {
                    if (s.isNullOrBlank()) {
                        model.showPopular(true)
                    } else {
                        model.searchSubreddits(s.toString())
                        model.showPopular(false)
                    }
                }
                handler.postDelayed(workRunnable ?: return, DELAY)
            }

        })
    }

    private fun initPopularItems() {
        val recycler = binding?.fragmentSearchPopularList
        val scrollListener = ListingScrollListener(
            15,
            binding?.fragmentSearchPopularList?.layoutManager as GridLayoutManager,
            model::loadMore
        )
        val listAdapter = ListingAdapter(
            markwon = null,
            subredditActions = null,
            expected = ItemType.Subreddit,
            itemClickListener = this,
            username = null
        ) {
            recycler?.apply {
                clearOnScrollListeners()
                addOnScrollListener(scrollListener)
                model.retry()
            }
        }
        recycler?.apply {
            adapter = listAdapter
            addOnScrollListener(scrollListener)
        }

        model.items.observe(viewLifecycleOwner, {
            listAdapter.submitList(it)
            scrollListener.finishedLoading()
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
        })

        model.lastItemReached.observe(viewLifecycleOwner, {
            if (it == true) {
                recycler?.clearOnScrollListeners()
            }
        })
    }

    private fun initSearchRecyclerViewAdapter() {
        val adapter = SimpleItemAdapter(null, this)
        val searchList = binding?.fragmentSearchSearchList
        searchList?.adapter = adapter

        model.searchItems.observe(viewLifecycleOwner, {
            adapter.submitList(it)
            searchList?.smoothScrollToPosition(0)
        })
    }

    private fun initOnClickListeners() {
        binding?.fragmentSearchToolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setMultiSelect() {

        val adapter = SelectedItemsAdapter {
            model.removeSelectedItem(it)
        }

        binding?.fragmentSearchSearchText?.imeOptions = EditorInfo.IME_ACTION_DONE

        binding?.fragmentSearchSelectedItems?.adapter = adapter

        model.selectedItems.observe(viewLifecycleOwner, {
            adapter.submitList(ArrayList(it))
        })

        binding?.fragmentSearchFab?.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(
                SELECTED_SUBREDDITS_KEY,
                model.selectedItems.value?.toList() ?: listOf()
            )
            navController.popBackStack()
        }
    }

    private fun setPostSearch() {
        binding?.fragmentSearchFab?.visibility = View.GONE
        binding?.fragmentSearchSearchText?.apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { textView, _, _ ->
                val query = textView.text.toString()
                findNavController().navigate(
                    SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(
                        ListingPage(SearchListing(query))
                    )
                )
                true
            }
        }
        binding?.fragmentSearchSearchButton?.setOnClickListener {
            val query = binding?.fragmentSearchSearchText?.text.toString()
            findNavController().navigate(
                SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(
                    ListingPage(SearchListing(query))
                )
            )
        }
    }

    override fun itemClicked(item: Item, position: Int) {
        val multiSelectMode = args.multiSelectMode
        if (multiSelectMode) {
            val name = when (item) {
                is Subreddit -> item.displayName
                is Account -> item.subreddit?.displayName ?: ""
                else -> throw IllegalStateException("Invalid account: $item")
            }
            model.addSelectedItem(name)
        } else {
            if (item is Account) {
                findNavController().navigate(
                    SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(
                        AccountPage(item.name)
                    )
                )
            } else {
                findNavController().navigate(
                    SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(
                        ListingPage(
                            SubredditListing((item as Subreddit).displayName)
                        )
                    )
                )
            }
        }
    }

    companion object {
        fun newInstance(): SearchFragment {
            return SearchFragment()
        }
    }
}