package dev.gtcl.reddit.ui.fragments.subreddits

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.ProfileInfo
import dev.gtcl.reddit.R
import dev.gtcl.reddit.SubscriptionType
import dev.gtcl.reddit.actions.ListingTypeClickListener
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.actions.SubscriptionActions
import dev.gtcl.reddit.actions.SubscriptionAdapterActions
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.models.reddit.All
import dev.gtcl.reddit.models.reddit.FrontPage
import dev.gtcl.reddit.models.reddit.Popular
import dev.gtcl.reddit.models.reddit.ProfileListing
import dev.gtcl.reddit.ui.viewholders.ListingVH
import dev.gtcl.reddit.ui.viewholders.SectionHeader
import dev.gtcl.reddit.ui.viewholders.SectionHeaderVH
import dev.gtcl.reddit.ui.viewholders.SubscriptionVH

class SubscriptionsAdapter(
    private val context: Context,
    private val listingTypeClickListener: ListingTypeClickListener,
    private val subscriptionActions: SubscriptionActions)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    SubscriptionAdapterActions {

    // Favorites - 0
    //  - List of favorites
    // Multi-Reddits
    //  - Front Page
    //  - All
    //  - Popular
    //  - Saved
    // Subreddits
    //  - List of Subreddits
    // Users
    // - List of Users

    private val favSectionHeader = object: SectionHeader(context.getString(R.string.favorites)){
        override fun onCollapse(collapse: Boolean) {
            if(favSubs.isEmpty()){
                return
            }
            if(collapse){
                notifyItemRangeRemoved(favHeaderIndex + 1, favSubs.size)
            } else {
                notifyItemRangeInserted(favHeaderIndex + 1, favSubs.size)
            }
        }
    }
    private var favSubs: MutableList<Subscription> = mutableListOf()
    private val favHeaderIndex: Int
        get(){
            return if(favSubs.isEmpty()){
                -1
            } else {
                0
            }
        }

    private val multisHeaderIndex: Int
        get(){
            var index = 0
            if(favSubs.isNotEmpty()){
                index++
                if(!favSectionHeader.isCollapsed){
                    index += favSubs.size
                }
            }
            return index
        }
    private val multiSectionHeader = object: SectionHeader(context.getString(R.string.multireddits)){
        override fun onCollapse(collapse: Boolean) {
            if(collapse){
                notifyItemRangeRemoved(multisHeaderIndex + 1, 4 + multis.size)
            } else {
                notifyItemRangeInserted(multisHeaderIndex + 1, 4 + multis.size)
            }
        }
    }
    private var multis : MutableList<Subscription> = mutableListOf()

    private val subredditsSectionHeader = object: SectionHeader(context.getString(R.string.subreddits)){
        override fun onCollapse(collapse: Boolean) {
            if(collapse){
                notifyItemRangeRemoved(subredditsHeaderIndex + 1, subreddits.size)
            } else {
                notifyItemRangeInserted(subredditsHeaderIndex + 1, subreddits.size)
            }
        }
    }
    private var subreddits: MutableList<Subscription> = mutableListOf()
    private val subredditsHeaderIndex: Int
        get(){
            var idx = 1 // Multi-Reddit header
            if(favSubs.isNotEmpty()){
                idx++
                if(!favSectionHeader.isCollapsed){
                    idx += favSubs.size
                }
            }
            if(!multiSectionHeader.isCollapsed) {
                idx += 4 + multis.size
            }
            return idx
        }

    private val usersSectionHeader = object: SectionHeader(context.getString(R.string.users)){
        override fun onCollapse(collapse: Boolean) {
            if(users.isEmpty()){
                return
            }
            if(collapse){
                notifyItemRangeRemoved(usersHeaderIndex + 1, users.size)
            } else {
                notifyItemRangeInserted(usersHeaderIndex + 1, users.size)
            }
        }
    }
    private var users: MutableList<Subscription> = mutableListOf()
    private val usersHeaderIndex: Int
        get(){
            if(users.isEmpty()){
                return -1
            }

            var idx = 2 // Multi-Reddits and Subreddits headers
            if(favSubs.isNotEmpty()){
                idx++
                if(!favSectionHeader.isCollapsed){
                    idx += favSubs.size
                }
            }

            if(!multiSectionHeader.isCollapsed) {
                idx += 4 + multis.size
            }

            if(!subredditsSectionHeader.isCollapsed){
                idx += subreddits.size
            }

            return idx
        }

    fun setSubscribedSubs(subs: List<Subscription>){
        val previousSize = subreddits.size
        subreddits.clear()
        if(!subredditsSectionHeader.isCollapsed){
            notifyItemRangeRemoved(subredditsHeaderIndex + 1, previousSize)
        }
        subreddits = subs.toMutableList()
        if(!subredditsSectionHeader.isCollapsed){
            notifyItemRangeInserted(subredditsHeaderIndex + 1, subreddits.size)
        }
    }

    fun setMultiReddits(multis: List<Subscription>){
        val previousSize = this.multis.size
        this.multis.clear()
        if(!multiSectionHeader.isCollapsed){
            notifyItemRangeRemoved(multisHeaderIndex + 5, previousSize)
        }
        this.multis = multis.toMutableList()
        if(!multiSectionHeader.isCollapsed){
            notifyItemRangeInserted(multisHeaderIndex + 5, multis.size)
        }
    }

    fun setFavorites(faves: List<Subscription>){
        val previousSize = this.favSubs.size
        this.favSubs.clear()
        if(previousSize != 0){
            if(!favSectionHeader.isCollapsed){
                notifyItemRangeRemoved(favHeaderIndex, previousSize + 1)
            } else {
                notifyItemRemoved(favHeaderIndex)
            }
        }

        favSubs = faves.toMutableList()
        if(faves.isNotEmpty()){
            if(!favSectionHeader.isCollapsed){
                notifyItemRangeInserted(favHeaderIndex, faves.size + 1)
            } else {
                notifyItemInserted(favHeaderIndex)
            }
        }
    }

    fun setUsers(users: List<Subscription>){
        val previousSize = this.users.size
        this.users.clear()
        if(previousSize != 0){
            if(!usersSectionHeader.isCollapsed){
                notifyItemRangeRemoved(usersHeaderIndex, previousSize + 1)
            } else {
                notifyItemRemoved(usersHeaderIndex)
            }
        }
        this.users = users.toMutableList()
        if(users.isNotEmpty()){
            if(!usersSectionHeader.isCollapsed){
                notifyItemRangeInserted(usersHeaderIndex, users.size + 1)
            } else {
                notifyItemInserted(usersHeaderIndex)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_section_header -> SectionHeaderVH.create(parent)
            R.layout.item_listing -> ListingVH.create(parent)
            R.layout.item_subscription -> SubscriptionVH.create(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when{
            position > usersHeaderIndex && usersHeaderIndex != -1 -> (holder as SubscriptionVH).bind(users[position - usersHeaderIndex - 1], listingTypeClickListener, this, subscriptionActions)
            position == usersHeaderIndex -> (holder as SectionHeaderVH).bind(usersSectionHeader)
            position > subredditsHeaderIndex -> (holder as SubscriptionVH).bind(subreddits[position - subredditsHeaderIndex - 1], listingTypeClickListener, this, subscriptionActions)
            position == subredditsHeaderIndex -> (holder as SectionHeaderVH).bind(subredditsSectionHeader)
            position > multisHeaderIndex + 4 -> (holder as SubscriptionVH).bind(multis[position - multisHeaderIndex - 4 - 1], listingTypeClickListener, this, subscriptionActions)
            position == multisHeaderIndex + 4 -> (holder as ListingVH).bind(ProfileListing(ProfileInfo.SAVED), listingTypeClickListener)
            position == multisHeaderIndex + 3 -> (holder as ListingVH).bind(Popular, listingTypeClickListener)
            position == multisHeaderIndex + 2 -> (holder as ListingVH).bind(All, listingTypeClickListener)
            position == multisHeaderIndex + 1 -> (holder as ListingVH).bind(FrontPage, listingTypeClickListener)
            position == multisHeaderIndex -> (holder as SectionHeaderVH).bind(multiSectionHeader)
            position > favHeaderIndex -> (holder as SubscriptionVH).bind(favSubs[position - favHeaderIndex -1], listingTypeClickListener, this, subscriptionActions, true)
            position == favHeaderIndex -> (holder as SectionHeaderVH).bind(favSectionHeader)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when{
            position == subredditsHeaderIndex
                    || position == multisHeaderIndex
                    || position == favHeaderIndex
                    || position == usersHeaderIndex ->{
                R.layout.item_section_header
            }
            position > subredditsHeaderIndex -> {
                R.layout.item_subscription
            }
            position > multisHeaderIndex -> {
                if(position <= multisHeaderIndex + 4){
                    R.layout.item_listing
                } else {
                    R.layout.item_subscription
                }
            } else -> {
                R.layout.item_subscription
            }
        }
    }

    override fun addToFavorites(sub: Subscription) {
        if(favSubs.isEmpty()){
            favSubs.add(sub)
            val addCount = if(!favSectionHeader.isCollapsed){
                2
            } else {
                1
            }
            notifyItemRangeInserted(favHeaderIndex,addCount)
        }
        else{
            val addingPosition = binarySearchPreviousHighest(favSubs, sub)
            favSubs.add(addingPosition, sub)
            if(!favSectionHeader.isCollapsed){
                notifyItemInserted(favHeaderIndex + 1 + addingPosition)
            }
        }
    }

    override fun removeFromFavorites(sub: Subscription, updateOtherSections: Boolean) {
        // Remove from Favorites section
        removeFromFavoritesListAndNotify(sub)

        // Remove from other sections
        if(!updateOtherSections) {
            return
        }

        when(sub.type){
            SubscriptionType.SUBREDDIT -> {
                val changePosition = binarySearch(subreddits, sub)
                if(changePosition != -1){
                    subreddits[changePosition].isFavorite = false
                    notifyItemChanged(subredditsHeaderIndex + 1 + changePosition)
                }
            }
            SubscriptionType.USER -> {
                val changePosition = binarySearch(users, sub)
                if(changePosition != -1){
                    users[changePosition].isFavorite = false
                    notifyItemChanged(usersHeaderIndex + 1 + changePosition)
                }
            }
            SubscriptionType.MULTIREDDIT -> {
                val changePosition = binarySearch(multis, sub)
                if(changePosition != -1){
                    multis[changePosition].isFavorite = false
                    notifyItemChanged(multisHeaderIndex + 5 + changePosition)
                }
            }
        }
    }

    override fun remove(sub: Subscription) {
        // Remove from Favorites section
        removeFromFavoritesListAndNotify(sub)

        // Remove from all Subreddits section
        val changePosition =
            binarySearch(
                subreddits,
                sub
            )
        if(changePosition != -1){
            subreddits.removeAt(changePosition)
            notifyItemRemoved(subredditsHeaderIndex + 1 + changePosition)
        }
    }

    private fun removeFromFavoritesListAndNotify(sub: Subscription){
        val removingPosition = binarySearch(favSubs, sub)
        if(removingPosition != -1){
            if(favSubs.size == 1){
                val headerIndex = favHeaderIndex
                favSubs.clear()
                val removalCount = if(!favSectionHeader.isCollapsed){
                    2
                } else {
                    1
                }
                notifyItemRangeRemoved(headerIndex, removalCount)
            } else {
                favSubs.removeAt(removingPosition)
                if(!favSectionHeader.isCollapsed){
                    notifyItemRemoved(favHeaderIndex + 1 + removingPosition)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        if(favSubs.isNotEmpty()){
            count++
            if(!favSectionHeader.isCollapsed){
                count += favSubs.size
            }
        }
        count++ // MultiReddits header
        if(!multiSectionHeader.isCollapsed){
            count += 4 + multis.size
        }
        count++ // All Subscriptions header
        if(!subredditsSectionHeader.isCollapsed){
            count += subreddits.size
        }
        if(users.isNotEmpty()){
            count++
            if(!usersSectionHeader.isCollapsed){
                count += users.size
            }
        }
        return count
    }

    companion object{
        private const val possibleDuplicates = 2

        fun binarySearch(list: List<Subscription>, sub: Subscription): Int{
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
                        for(i in (mid - possibleDuplicates)..(mid + possibleDuplicates)){
                            if(i >= 0 && i < list.size){
                                if(list[i].id == sub.id){
                                    return i
                                }
                            }
                        }
                        return -1
                    }
                }
            }
            return -1
        }

        fun binarySearchPreviousHighest(list: List<Subscription>, sub: Subscription): Int{
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

}
