package dev.gtcl.reddit.ui.fragments.subreddits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dev.gtcl.reddit.RedditApplication
import dev.gtcl.reddit.ViewModelFactory
import dev.gtcl.reddit.actions.ListingTypeClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.databinding.FragmentDialogMySubredditsBinding
import dev.gtcl.reddit.models.reddit.ListingType
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.ui.fragments.subreddits.mine.MineFragment
import dev.gtcl.reddit.ui.fragments.subreddits.mine.MySubredditsAdapter
import dev.gtcl.reddit.ui.fragments.subreddits.mine.MySubredditsVM

class MyFavoritesDialogFragment: BottomSheetDialogFragment(), SubredditActions,
    ListingTypeClickListener {

    private lateinit var binding: FragmentDialogMySubredditsBinding
    private var parentListingTypeClickListener: ListingTypeClickListener? = null
    private var parentSubredditActions: SubredditActions? = null

    fun setActions(listingTypeClickListener: ListingTypeClickListener, subredditActions: SubredditActions){
        this.parentListingTypeClickListener = listingTypeClickListener
        this.parentSubredditActions = subredditActions
    }

    val model: MySubredditsVM by lazy {
        val viewModelFactory = ViewModelFactory(requireActivity(). application as RedditApplication)
        ViewModelProvider(this, viewModelFactory).get(MySubredditsVM::class.java)
    }

    override fun onResume() {
        super.onResume()
        model.syncWithDb()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDialogMySubredditsBinding.inflate(inflater)
        setRecyclerView()
        setListeners()
        return binding.root
    }

    private fun setRecyclerView(){
        val adapter = MySubredditsAdapter(requireContext(), this, this)
        binding.recyclerView.adapter = adapter

        model.subscribedSubs.observe(viewLifecycleOwner, Observer {
            if(it != null) {
                adapter.setSubscribedSubs(it)
                model.subredditsSynced()
            }
        })

    }

    private fun setListeners(){
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
    }

    companion object{
        fun newInstance(): MineFragment {
            return MineFragment()
        }
    }

//      _____       _                  _     _ _ _                  _   _
//     / ____|     | |                | |   | (_) |       /\       | | (_)
//    | (___  _   _| |__  _ __ ___  __| | __| |_| |_     /  \   ___| |_ _  ___  _ __  ___
//     \___ \| | | | '_ \| '__/ _ \/ _` |/ _` | | __|   / /\ \ / __| __| |/ _ \| '_ \/ __|
//     ____) | |_| | |_) | | |  __/ (_| | (_| | | |_   / ____ \ (__| |_| | (_) | | | \__ \
//    |_____/ \__,_|_.__/|_|  \___|\__,_|\__,_|_|\__| /_/    \_\___|\__|_|\___/|_| |_|___/
//

    override fun favorite(subreddit: Subreddit, favorite: Boolean) {
        parentSubredditActions?.favorite(subreddit, favorite)
    }

    override fun subscribe(subreddit: Subreddit, subscribe: Boolean) {
        parentSubredditActions?.subscribe(subreddit, subscribe)
    }

    override fun listingTypeClicked(listing: ListingType) {
        parentListingTypeClickListener?.listingTypeClicked(listing)
    }

}