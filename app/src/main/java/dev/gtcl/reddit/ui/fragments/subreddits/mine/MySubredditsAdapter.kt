package dev.gtcl.reddit.ui.fragments.subreddits.mine

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.R
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.ListingTypeClickListener
import dev.gtcl.reddit.actions.MySubredditAdapterActions
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.database.DbMultiReddit
import dev.gtcl.reddit.models.reddit.*
import dev.gtcl.reddit.ui.viewholders.ListingVH
import dev.gtcl.reddit.ui.viewholders.MultiRedditVH
import dev.gtcl.reddit.ui.viewholders.SectionHeaderVH
import dev.gtcl.reddit.ui.viewholders.SubredditVH

class MySubredditsAdapter(private val context: Context, private val listingTypeClickListener: ListingTypeClickListener, private val subredditActions: SubredditActions) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    MySubredditAdapterActions, ItemClickListener {

    // Multi-Reddits 0
    //  - Front Page 1
    //  - All 2
    //  - Popular 3
    //  - Saved 4
    //  - Multi-Reddits
    // Favorites
    //  - List of favorites
    // Subreddits
    // - List of subreddits

    private val multisHeaderIndex = 0
    private val multiSectionHeader = object: SectionHeader(context.getString(R.string.multireddits), false){
        override fun onCollapse(collapse: Boolean) {
            if(collapse){
                notifyItemRangeRemoved(1, 4 + multis.size)
            } else {
                notifyItemRangeInserted(1, 4 + multis.size)
            }
        }
    }
    private var multis : MutableList<DbMultiReddit> = mutableListOf()

    private val favSectionHeader = object: SectionHeader(context.getString(R.string.favorites), false){
        override fun onCollapse(collapse: Boolean) {
            if(favSubs.isEmpty()){
                return
            }
            if(collapse){
                notifyItemRangeRemoved(favoriteSubsHeaderIndex + 1, favSubs.size)
            } else {
                notifyItemRangeInserted(favoriteSubsHeaderIndex + 1, favSubs.size)
            }
        }
    }
    private var favSubs: MutableList<Subreddit> = mutableListOf()

    private val favoriteSubsHeaderIndex: Int
        get(){
            if(favSubs.isEmpty()){
                return -1
            }
            var idx = 1
            if(multiSectionHeader.isCollapsed != true) {
                idx += 4 + multis.size
            }
            return idx
        }

    private val allSubsSectionHeader = object: SectionHeader(context.getString(R.string.subreddits)){
        override fun onCollapse(collapse: Boolean) {}
    }
    private var allSubs: MutableList<Subreddit> = mutableListOf()


    private val allSubsHeaderIndex: Int
        get(){
            var idx = 1
            if(multiSectionHeader.isCollapsed != true) {
                idx += 4 + multis.size
            }
            if(favSubs.isNotEmpty()){
                idx++
                if(favSectionHeader.isCollapsed != true){
                    idx += favSubs.size
                }
            }
            return idx
        }

    fun setSubscribedSubs(subs: List<Subreddit>){
        allSubs = subs.toMutableList()
        favSubs = subs.filter { subreddit -> subreddit.isFavorite }.toMutableList()
        notifyDataSetChanged()
    }

    fun setMultiReddits(multis: List<DbMultiReddit>){
        this.multis = multis.toMutableList()
        notifyDataSetChanged()
    }

    override fun addToFavorites(sub: Subreddit) {
        if(favSubs.isEmpty()){
            favSubs.add(sub)
            val addCount = if(favSectionHeader.isCollapsed != true){
                2
            } else {
                1
            }
            notifyItemRangeInserted(favoriteSubsHeaderIndex,addCount)
        }
        else{
            val addingPosition = binarySearchPreviousHighest(favSubs, sub)
            favSubs.add(addingPosition, sub)
            if(favSectionHeader.isCollapsed != true){
                notifyItemInserted(favoriteSubsHeaderIndex + 1 + addingPosition)
            }
        }
    }

    override fun removeFromFavorites(sub: Subreddit, updateAllSubredditsSection: Boolean) {
        // Remove from Favorites section
        removeFromFavoritesListAndNotify(sub)

        // Remove from all Subreddits section
        if(!updateAllSubredditsSection){
            return
        }
        val changePosition = binarySearch(allSubs, sub)
        if(changePosition != -1){
            notifyItemChanged(allSubsHeaderIndex + 1 + changePosition)
        }
    }

    override fun remove(sub: Subreddit) {
        // Remove from Favorites section
        removeFromFavoritesListAndNotify(sub)

        // Remove from all Subreddits section
        val changePosition = binarySearch(allSubs, sub)
        if(changePosition != -1){
            allSubs.removeAt(changePosition)
            notifyItemRemoved(allSubsHeaderIndex + 1 + changePosition)
        }
    }

    private fun removeFromFavoritesListAndNotify(sub: Subreddit){
        val removingPosition = binarySearch(favSubs, sub)
        if(removingPosition != -1){
            if(favSubs.size == 1){
                val headerIndex = favoriteSubsHeaderIndex
                favSubs.clear()
                val removalCount = if(favSectionHeader.isCollapsed != true){
                    2
                } else {
                    1
                }
                notifyItemRangeRemoved(headerIndex, removalCount)
            } else {
                favSubs.removeAt(removingPosition)
                if(favSectionHeader.isCollapsed != true){
                    notifyItemRemoved(favoriteSubsHeaderIndex + 1 + removingPosition)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_section_header -> SectionHeaderVH.create(parent)
            R.layout.item_listing -> ListingVH.create(parent)
            R.layout.item_subreddit -> SubredditVH.create(parent)
            R.layout.item_multireddit -> MultiRedditVH.create(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when{
            position > allSubsHeaderIndex -> (holder as SubredditVH).bind(allSubs[position - allSubsHeaderIndex - 1], subredditActions, this, false, this)
            position == allSubsHeaderIndex -> (holder as SectionHeaderVH).bind(allSubsSectionHeader)
            position > favoriteSubsHeaderIndex && favoriteSubsHeaderIndex != -1 -> (holder as SubredditVH).bind(favSubs[position - favoriteSubsHeaderIndex - 1], subredditActions, this, true, this)
            position == favoriteSubsHeaderIndex && favoriteSubsHeaderIndex != -1 -> (holder as SectionHeaderVH).bind(favSectionHeader)
            position == multisHeaderIndex -> (holder as SectionHeaderVH).bind(multiSectionHeader, false)
            position == multisHeaderIndex + 1 -> (holder as ListingVH).bind(FrontPage, listingTypeClickListener)
            position == multisHeaderIndex + 2 -> (holder as ListingVH).bind(All, listingTypeClickListener)
            position == multisHeaderIndex + 3 -> (holder as ListingVH).bind(Popular, listingTypeClickListener)
            position == multisHeaderIndex + 4 -> (holder as ListingVH).bind(ProfileListing(ProfileInfo.SAVED), listingTypeClickListener)
            else -> (holder as MultiRedditVH).bind(multis[position - 5], this)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when{
            position == allSubsHeaderIndex
                    || position == favoriteSubsHeaderIndex
                    || position == multisHeaderIndex -> {
                R.layout.item_section_header
            }
            position > allSubsHeaderIndex-> {
                R.layout.item_subreddit
            }
            favoriteSubsHeaderIndex != -1 && position > favoriteSubsHeaderIndex -> {
                R.layout.item_subreddit
            }
            (multiSectionHeader.isCollapsed != true) && position > multisHeaderIndex && position <= multisHeaderIndex + 4 -> {
                R.layout.item_listing
            }
            else -> {
                R.layout.item_multireddit
            }
        }
    }

    override fun getItemCount(): Int {
        var count = 1 // Multi-Reddit Header
        if(multiSectionHeader.isCollapsed != true){
            count += 4 + multis.size // FrontPage, All, Popular, Saved
        }
        if(favSubs.isNotEmpty()){
            count++ // Header
            if(favSectionHeader.isCollapsed != true){
                count += favSubs.size
            }
        }
        count++ // All Subs Header
        count += allSubs.size

        return count
    }

    companion object{
        fun binarySearch(list: List<Subreddit>, sub: Subreddit): Int{
            var lo = 0
            var hi = list.size - 1
            while(lo <= hi){
                val mid = lo + (hi - lo)/2
                val cmp = sub.displayName.compareTo(list[mid].displayName, true)
                when {
                    cmp < 0 -> {
                        hi = mid - 1
                    }
                    cmp > 0 -> {
                        lo = mid + 1
                    }
                    else -> {
                        return mid
                    }
                }
            }
            return -1
        }

        fun binarySearchPreviousHighest(list: List<Subreddit>, sub: Subreddit): Int{
            var lo = 0
            var hi = list.size - 1
            var result = -1
            while(lo <= hi){
                val mid = lo + (hi - lo)/2
                val cmp = sub.displayName.compareTo(list[mid].displayName, true)
                when {
                    cmp <= 0 -> {
                        hi = mid - 1
                        result = mid
                    }
                    cmp > 0 -> {
                        lo = mid + 1
                    }
                }
            }
            return if(result != -1) {
                result
            } else {
                list.size
            }
        }

    }

    override fun itemClicked(item: Item) {
        when(item){
            is Subreddit -> {
                listingTypeClickListener.listingTypeClicked(SubredditListing(item))
            }
            is MultiReddit -> {
                listingTypeClickListener.listingTypeClicked(MultiRedditListing(item.asDbModel()))
            }
        }
    }
}

abstract class SectionHeader(
    val name: String,
    collapsed: Boolean? = null
) {
    var isCollapsed: Boolean? = collapsed
        set(collapse){
            field = collapse
            if(collapse != null){
                onCollapse(collapse)
            }
        }

    abstract fun onCollapse(collapse: Boolean)
}