package dev.gtcl.reddit.ui.fragments.subreddits.search

import android.content.Context
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
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.databinding.FragmentSearchBinding
import dev.gtcl.reddit.models.reddit.Account
import dev.gtcl.reddit.models.reddit.Item
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.models.reddit.SubredditListing
import dev.gtcl.reddit.network.NetworkState
import dev.gtcl.reddit.ui.activities.MainActivityVM
import dev.gtcl.reddit.ui.fragments.AccountPage
import dev.gtcl.reddit.ui.fragments.ListingPage


class SearchFragment : Fragment(), ItemClickListener, SubredditActions {
    private lateinit var binding: FragmentSearchBinding

    private val model: SearchVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity().application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(SearchVM::class.java)
    }

    private val activityModel: MainActivityVM by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSearchBinding.inflate(inflater)
        setEditTextListener()
        setRecyclerViewAdapter()
        setOnClickListeners()
        showKeyboard()
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
                    if(!s.isNullOrBlank()){
                        model.searchSubreddits(s.toString())
                    }
                }
                handler.postDelayed(workRunnable!!, DELAY)
            }

        })
    }

    private fun setRecyclerViewAdapter(){
        val adapter = SearchAdapter(this, this)
        binding.list.adapter = adapter

        model.searchItems.observe(viewLifecycleOwner, Observer {
            if(it != null){
                adapter.submitList(it)
                binding.list.smoothScrollToPosition(0)
                binding.list.visibility = if(it.isEmpty()) View.GONE else View.VISIBLE
                binding.noResultsText.visibility = if(it.isNotEmpty()) View.GONE else View.VISIBLE
                model.searchComplete()
            }
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

    override fun itemClicked(item: Item) {
        if(item is Account){
            findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(AccountPage(item.name)))
        } else {
            findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToViewPagerFragment(ListingPage(SubredditListing(item as Subreddit))))
        }
        hideKeyboard()
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