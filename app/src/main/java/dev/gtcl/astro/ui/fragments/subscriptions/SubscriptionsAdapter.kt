package dev.gtcl.astro.ui.fragments.subscriptions

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.ProfileInfo
import dev.gtcl.astro.R
import dev.gtcl.astro.SubscriptionType
import dev.gtcl.astro.actions.ListingTypeClickListener
import dev.gtcl.astro.actions.SubscriptionActions
import dev.gtcl.astro.actions.SubscriptionAdapterActions
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.models.reddit.listing.*
import dev.gtcl.astro.ui.viewholders.ExpandableHeaderVH
import dev.gtcl.astro.ui.viewholders.ExpandableItem
import dev.gtcl.astro.ui.viewholders.ListingVH
import dev.gtcl.astro.ui.viewholders.SubscriptionVH

class SubscriptionsAdapter(
    private val context: Context,
    private val listingTypeClickListener: ListingTypeClickListener,
    private val subscriptionActions: SubscriptionActions
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    SubscriptionAdapterActions {

    // Favorites - 0
    //  - List of favorites
    // Multi-Reddits
    //  - Front Page
    //  - All
    //  - Popular
    //  - Saved
    //  - Friends
    // Subreddits
    //  - List of Subreddits
    // Users
    // - List of Users

    private val defaultMultis = 5

    private val favHeader = object : ExpandableItem(context.getString(R.string.favorites)) {
        override fun onExpand(expand: Boolean) {
            if (favSubs.isEmpty()) {
                return
            }
            if (expand) {
                notifyItemRangeInserted(favHeaderIndex + 1, favSubs.size)
            } else {
                notifyItemRangeRemoved(favHeaderIndex + 1, favSubs.size)
            }
        }
    }
    private var favSubs: MutableList<Subscription> = mutableListOf()
    private val favHeaderIndex: Int
        get() {
            return if (favSubs.isEmpty()) {
                -1
            } else {
                0
            }
        }

    private val multisHeaderIndex: Int
        get() {
            var index = 0
            if (favSubs.isNotEmpty()) {
                index++
                if (favHeader.expanded) {
                    index += favSubs.size
                }
            }
            return index
        }
    private val multiHeader = object : ExpandableItem(context.getString(R.string.multireddits)) {
        override fun onExpand(expand: Boolean) {
            if (expand) {
                notifyItemRangeInserted(multisHeaderIndex + 1, defaultMultis + multis.size)
            } else {
                notifyItemRangeRemoved(multisHeaderIndex + 1, defaultMultis + multis.size)
            }
        }
    }
    private var multis: MutableList<Subscription> = mutableListOf()

    private val subsHeader = object : ExpandableItem(context.getString(R.string.subreddits)) {
        override fun onExpand(expand: Boolean) {
            if (expand) {
                notifyItemRangeInserted(subredditsHeaderIndex + 1, subreddits.size)
            } else {
                notifyItemRangeRemoved(subredditsHeaderIndex + 1, subreddits.size)
            }
        }
    }
    private var subreddits: MutableList<Subscription> = mutableListOf()
    private val subredditsHeaderIndex: Int
        get() {
            var idx = 1 // Multi-Reddit header
            if (favSubs.isNotEmpty()) {
                idx++
                if (favHeader.expanded) {
                    idx += favSubs.size
                }
            }
            if (multiHeader.expanded) {
                idx += defaultMultis + multis.size
            }
            return idx
        }

    private val usersHeader = object : ExpandableItem(context.getString(R.string.users)) {
        override fun onExpand(expand: Boolean) {
            if (users.isEmpty()) {
                return
            }
            if (expand) {
                notifyItemRangeInserted(usersHeaderIndex + 1, users.size)
            } else {
                notifyItemRangeRemoved(usersHeaderIndex + 1, users.size)
            }
        }
    }
    private var users: MutableList<Subscription> = mutableListOf()
    private val usersHeaderIndex: Int
        get() {
            if (users.isEmpty()) {
                return -1
            }

            var idx = 2 // Multi-Reddits and Subreddits headers
            if (favSubs.isNotEmpty()) {
                idx++
                if (favHeader.expanded) {
                    idx += favSubs.size
                }
            }

            if (multiHeader.expanded) {
                idx += defaultMultis + multis.size
            }

            if (subsHeader.expanded) {
                idx += subreddits.size
            }

            return idx
        }

    fun setSubscriptions(
        favorites: List<Subscription>,
        multis: List<Subscription>,
        subs: List<Subscription>,
        users: List<Subscription>
    ) {
        this.favSubs = favorites.toMutableList()
        this.multis = multis.toMutableList()
        this.subreddits = subs.toMutableList()
        this.users = users.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_expandable -> ExpandableHeaderVH.create(parent)
            R.layout.item_listing -> ListingVH.create(parent)
            R.layout.item_subscription -> SubscriptionVH.create(parent)
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            position > usersHeaderIndex && usersHeaderIndex != -1 -> (holder as SubscriptionVH).bind(
                users[position - usersHeaderIndex - 1],
                listingTypeClickListener,
                this,
                subscriptionActions
            )
            position == usersHeaderIndex -> (holder as ExpandableHeaderVH).bind(
                usersHeader,
                position != 0
            )
            position > subredditsHeaderIndex -> (holder as SubscriptionVH).bind(
                subreddits[position - subredditsHeaderIndex - 1],
                listingTypeClickListener,
                this,
                subscriptionActions
            )
            position == subredditsHeaderIndex -> (holder as ExpandableHeaderVH).bind(
                subsHeader,
                position != 0
            )
            position > multisHeaderIndex + defaultMultis -> (holder as SubscriptionVH).bind(
                multis[position - multisHeaderIndex - defaultMultis - 1],
                listingTypeClickListener,
                this,
                subscriptionActions
            )
            position == multisHeaderIndex + defaultMultis -> (holder as ListingVH).bind(
                Friends, listingTypeClickListener
            )
            position == multisHeaderIndex + defaultMultis - 1 -> (holder as ListingVH).bind(
                ProfileListing(
                    null,
                    ProfileInfo.SAVED
                ), listingTypeClickListener
            )
            position == multisHeaderIndex + defaultMultis - 2 -> (holder as ListingVH).bind(
                Popular,
                listingTypeClickListener
            )
            position == multisHeaderIndex + defaultMultis - 3 -> (holder as ListingVH).bind(
                All,
                listingTypeClickListener
            )
            position == multisHeaderIndex + defaultMultis - 4 -> (holder as ListingVH).bind(
                FrontPage,
                listingTypeClickListener
            )
            position == multisHeaderIndex -> (holder as ExpandableHeaderVH).bind(
                multiHeader,
                position != 0
            )
            position > favHeaderIndex -> (holder as SubscriptionVH).bind(
                favSubs[position - favHeaderIndex - 1],
                listingTypeClickListener,
                this,
                subscriptionActions,
                true
            )
            position == favHeaderIndex -> (holder as ExpandableHeaderVH).bind(
                favHeader,
                position != 0
            )
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position == subredditsHeaderIndex
                    || position == multisHeaderIndex
                    || position == favHeaderIndex
                    || position == usersHeaderIndex -> {
                R.layout.item_expandable
            }
            position > subredditsHeaderIndex -> {
                R.layout.item_subscription
            }
            position > multisHeaderIndex -> {
                if (position <= multisHeaderIndex + defaultMultis) {
                    R.layout.item_listing
                } else {
                    R.layout.item_subscription
                }
            }
            else -> {
                R.layout.item_subscription
            }
        }
    }

    override fun addToFavorites(sub: Subscription) {
        if (favSubs.isEmpty()) {
            favSubs.add(sub)
            val addCount = if (favHeader.expanded) {
                2
            } else {
                1
            }
            notifyItemRangeInserted(favHeaderIndex, addCount)
            notifyItemChanged(multisHeaderIndex)
        } else {
            val addingPosition = binarySearchPreviousHighest(favSubs, sub)
            favSubs.add(addingPosition, sub)
            if (favHeader.expanded) {
                notifyItemInserted(favHeaderIndex + 1 + addingPosition)
            }
        }
    }

    override fun removeFromFavorites(sub: Subscription, updateOtherSections: Boolean) {
        // Remove from Favorites section
        removeFromFavoritesListAndNotify(sub)

        // Remove from other sections
        if (!updateOtherSections) {
            return
        }

        when (sub.type) {
            SubscriptionType.SUBREDDIT -> {
                val changePosition = binarySearch(subreddits, sub)
                if (changePosition != -1) {
                    subreddits[changePosition].isFavorite = false
                    notifyItemChanged(subredditsHeaderIndex + 1 + changePosition)
                }
            }
            SubscriptionType.USER -> {
                val changePosition = binarySearch(users, sub)
                if (changePosition != -1) {
                    users[changePosition].isFavorite = false
                    notifyItemChanged(usersHeaderIndex + 1 + changePosition)
                }
            }
            SubscriptionType.MULTIREDDIT -> {
                val changePosition = binarySearch(multis, sub)
                if (changePosition != -1) {
                    multis[changePosition].isFavorite = false
                    notifyItemChanged(multisHeaderIndex + defaultMultis + changePosition)
                }
            }
        }
    }

    override fun remove(sub: Subscription) {
        // Remove from Favorites section
        removeFromFavoritesListAndNotify(sub)

        // Remove from all Subreddits section
        when (sub.type) {
            SubscriptionType.SUBREDDIT -> {
                val changePosition =
                    binarySearch(
                        subreddits,
                        sub
                    )
                if (changePosition != -1) {
                    subreddits.removeAt(changePosition)
                    notifyItemRemoved(subredditsHeaderIndex + 1 + changePosition)
                }
            }
            SubscriptionType.MULTIREDDIT -> {
                val changePosition =
                    binarySearch(
                        multis,
                        sub
                    )
                if (changePosition != -1) {
                    multis.removeAt(changePosition)
                    notifyItemRemoved(multisHeaderIndex + defaultMultis + changePosition)
                }
            }
            SubscriptionType.USER -> {
                val changePosition =
                    binarySearch(
                        users,
                        sub
                    )
                if (changePosition != -1) {
                    users.removeAt(changePosition)
                    notifyItemRemoved(usersHeaderIndex + 1 + changePosition)
                }
            }
        }

    }

    private fun removeFromFavoritesListAndNotify(sub: Subscription) {
        val removingPosition = binarySearch(favSubs, sub)
        if (removingPosition != -1) {
            if (favSubs.size == 1) {
                val headerIndex = favHeaderIndex
                favSubs.clear()
                val removalCount = if (favHeader.expanded) {
                    2
                } else {
                    1
                }
                notifyItemRangeRemoved(headerIndex, removalCount)
                notifyItemChanged(multisHeaderIndex)
            } else {
                favSubs.removeAt(removingPosition)
                if (favHeader.expanded) {
                    notifyItemRemoved(favHeaderIndex + 1 + removingPosition)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        if (favSubs.isNotEmpty()) {
            count++
            if (favHeader.expanded) {
                count += favSubs.size
            }
        }
        count++ // MultiReddits header
        if (multiHeader.expanded) {
            count += defaultMultis + multis.size
        }
        count++ // All Subscriptions header
        if (subsHeader.expanded) {
            count += subreddits.size
        }
        if (users.isNotEmpty()) {
            count++
            if (usersHeader.expanded) {
                count += users.size
            }
        }
        return count
    }

    companion object {
        private const val possibleDuplicates = 2

        fun binarySearch(list: List<Subscription>, sub: Subscription): Int {
            var lo = 0
            var hi = list.size - 1
            while (lo <= hi) {
                val mid = lo + (hi - lo) / 2
                val cmp = sub.displayName.compareTo(list[mid].displayName, true)
                when {
                    cmp < 0 -> {
                        hi = mid - 1
                    }
                    cmp > 0 -> {
                        lo = mid + 1
                    }
                    else -> {
                        for (i in (mid - possibleDuplicates)..(mid + possibleDuplicates)) {
                            if (i >= 0 && i < list.size) {
                                if (list[i].id == sub.id) {
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

        fun binarySearchPreviousHighest(list: List<Subscription>, sub: Subscription): Int {
            var lo = 0
            var hi = list.size - 1
            var result = -1
            while (lo <= hi) {
                val mid = lo + (hi - lo) / 2
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
            return if (result != -1) {
                result
            } else {
                list.size
            }
        }

    }

}
