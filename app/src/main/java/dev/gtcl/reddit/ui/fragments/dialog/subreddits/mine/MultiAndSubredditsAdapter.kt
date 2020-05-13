package dev.gtcl.reddit.ui.fragments.dialog.subreddits.mine

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.SubsAdapterActions
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.actions.ListingActions
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.ui.viewholders.ListingViewHolder
import dev.gtcl.reddit.ui.viewholders.SectionTitleViewHolder
import dev.gtcl.reddit.ui.viewholders.SubredditViewHolder

class MultiAndSubredditsAdapter(private val context: Context, private val listingActions: ListingActions, private val subredditActions: SubredditActions) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    SubsAdapterActions {

    // Multi-Reddits 0
    //  - Front Page 1
    //  - All 2
    //  - Popular 3
    //  - Saved 4
    // Favorites? Remove if empty 5
    //  - List of favorites (if any) 6
    // Subreddits
    // - List of subreddits
    private var favSubs: MutableList<Subreddit> = mutableListOf()
    private var allSubs: MutableList<Subreddit> = mutableListOf()
    private val startingIndexOfSubs: Int
        get(){
            var start = 6 + favSubs.size
            if(favSubs.isNotEmpty()) start++
            return start
        }

    fun loadInitialSubreddits(subs: List<Subreddit>){
        allSubs = subs.toMutableList()
        favSubs = subs.filter { subreddit -> subreddit.isFavorite }.toMutableList()
        notifyDataSetChanged()
    }

    override fun addToFavorites(subreddit: Subreddit) {
        if(favSubs.isEmpty()){
            favSubs.add(subreddit)
            notifyItemRangeInserted(5,2)
        }
        else{
            var addingPosition = 0
            while(addingPosition < favSubs.size && favSubs[addingPosition].displayName.compareTo(subreddit.displayName, true) < 0) // TODO: Binary Search
                    addingPosition++
            favSubs.add(addingPosition, subreddit)
            notifyItemInserted(addingPosition + 6)
        }
    }

    override fun removeFromFavorites(subreddit: Subreddit) {
        if(favSubs.size == 1){
            favSubs.clear()
            notifyItemRangeRemoved(5, 2)
        }
        else {
            var removingPosition = 0
            while(removingPosition < favSubs.size && favSubs[removingPosition].displayName.compareTo(subreddit.displayName, true) != 0) // TODO: Binary Search
                removingPosition++
            favSubs.removeAt(removingPosition)
            notifyItemRemoved(removingPosition + 6)
        }

        for(x in 0 until allSubs.size){
            if(subreddit.displayName.compareTo(allSubs[x].displayName, true) == 0){ // TODO: Binary Search
                notifyItemChanged(x + startingIndexOfSubs)
                return
            }
        }
    }

    override fun remove(subreddit: Subreddit) {
        if(favSubs.size == 1){
            favSubs.clear()
            notifyItemRangeRemoved(5, 2)
        }
        else {
            var removingPosition = 0
            while(removingPosition < favSubs.size && favSubs[removingPosition].displayName.compareTo(subreddit.displayName, true) != 0) // TODO: Binary Search
                removingPosition++
            if(removingPosition < favSubs.size){
                favSubs.removeAt(removingPosition)
                notifyItemRemoved(removingPosition + 6)
            }
        }

        for(x in 0 until allSubs.size){ // TODO: Binary Search
            if(subreddit.displayName.compareTo(allSubs[x].displayName, true) == 0){
                allSubs.removeAt(x)
                notifyItemRemoved(x + startingIndexOfSubs)
                return
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_section_header -> SectionTitleViewHolder.create(parent)
            R.layout.item_listing -> ListingViewHolder.create(parent)
            R.layout.item_subreddit -> SubredditViewHolder.create(parent)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(favSubs.isEmpty()){
            when(position){
                0 -> (holder as SectionTitleViewHolder).bind(context.getString(R.string.multireddits)) // Multi-Reddits header
                1 -> (holder as ListingViewHolder).bind(FrontPage, listingActions)
                2 -> (holder as ListingViewHolder).bind(All, listingActions)
                3 -> (holder as ListingViewHolder).bind(Popular, listingActions)
                4 -> (holder as ListingViewHolder).bind(ProfileListing(ProfileInfo.SAVED), listingActions)
                5 -> (holder as SectionTitleViewHolder).bind(context.getString(R.string.subreddits)) // Subreddits header
                in 6 until 6 + allSubs.size -> (holder as SubredditViewHolder).bind(allSubs[position - 6], subredditActions, this)
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
        else {
            when(position){
                0 -> (holder as SectionTitleViewHolder).bind(context.getString(R.string.multireddits))  // Multi-Reddits header
                1 -> (holder as ListingViewHolder).bind(FrontPage, listingActions)
                2 -> (holder as ListingViewHolder).bind(All, listingActions)
                3 -> (holder as ListingViewHolder).bind(Popular, listingActions)
                4 -> (holder as ListingViewHolder).bind(ProfileListing(ProfileInfo.SAVED), listingActions)
                5 -> (holder as SectionTitleViewHolder).bind(context.getString(R.string.favorites)) // Favorites header
                in 6 until 6 + favSubs.size -> (holder as SubredditViewHolder).bind(favSubs[position - 6], subredditActions, this)  // Favorite Subreddits
                6 + favSubs.size -> (holder as SectionTitleViewHolder).bind(context.getString(R.string.subreddits)) // Subreddits header
                in (6 + favSubs.size + 1)..(6 + favSubs.size + allSubs.size) -> (holder as SubredditViewHolder).bind(allSubs[position - (6 + favSubs.size + 1)], subredditActions, this) // Non-favorite subreddits
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if(favSubs.isEmpty()){
            return when(position){
                0 -> R.layout.item_section_header // Multi-Reddits header
                in 1..4 -> R.layout.item_listing // Front Page, All, Popular, Saved
                5 -> R.layout.item_section_header // Subreddits header
                in 6 until 6 + allSubs.size -> R.layout.item_subreddit // Non-Favorite Subreddits
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
        else {
            return when(position){
                0 -> R.layout.item_section_header // Multi-Reddits header
                in 1..4 -> R.layout.item_listing // Front Page, All, Popular, Saved
                5 -> R.layout.item_section_header // Favorites header
                in 6 until 6 + favSubs.size -> R.layout.item_subreddit // Favorite Subreddits
                6 + favSubs.size -> R.layout.item_section_header // Subreddits header
                in (6 + favSubs.size + 1)..(6 + favSubs.size + allSubs.size) -> R.layout.item_subreddit // Non-favorite subreddits
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }

    override fun getItemCount(): Int {
        var count = 5 // Multi-Reddits, Front Page, All, Popular, Saved
        count += if(favSubs.isEmpty()) 0 else 1 // Favorites header
        count += favSubs.size
        count += 1 // Subreddit Header
        count += allSubs.size
        return count
    }

}