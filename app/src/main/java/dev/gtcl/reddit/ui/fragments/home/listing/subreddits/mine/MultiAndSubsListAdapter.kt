package dev.gtcl.reddit.ui.fragments.home.listing.subreddits.mine

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.listings.ListingType
import dev.gtcl.reddit.listings.Subreddit
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.ListingOnClickListeners
import dev.gtcl.reddit.ui.viewholders.ListingViewHolder
import dev.gtcl.reddit.ui.viewholders.SectionTitleViewHolder
import dev.gtcl.reddit.ui.viewholders.SubredditViewHolder

class MultiAndSubsListAdapter(private val context: Context, private val listingOnClickListeners: ListingOnClickListeners) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    // Multi-Reddits
    //  - Front Page
    //  - All
    //  - Popular
    //  - Saved
    // Favorites? Remove if empty
    //  - List of favorites (if any)
    // Subreddits
    // - List of subreddits

    private var mItems: MutableList<Any> = mutableListOf()
    private var favoriteSubs: HashSet<String> = hashSetOf()

    fun submitLists(multiReddits: List<ListingType>, subs: List<Subreddit>){
        mItems = mutableListOf()
        mItems.add(context.resources.getText(R.string.multireddits).toString())
        mItems.addAll(multiReddits)
        mItems.add(context.resources.getText(R.string.subreddits).toString())
        mItems.addAll(subs)
        notifyDataSetChanged()
    }

    fun submitFavorites(subs: List<Subreddit>){
        favoriteSubs = subs.map { it.displayName }.toHashSet()
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
        when(val item = mItems[position]) {
            is String -> (holder as SectionTitleViewHolder).bind(item)
            is ListingType -> (holder as ListingViewHolder).bind(item, listingOnClickListeners)
            is Subreddit -> (holder as SubredditViewHolder).bind(item, listingOnClickListeners, true) // TODO: Change added parameter
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(mItems[position]){
            is String -> R.layout.item_section_header
            is ListingType -> R.layout.item_listing
            is Subreddit -> R.layout.item_subreddit
            else -> throw IllegalArgumentException("Invalid item: ${mItems[position].javaClass.simpleName}")
        }
    }

    override fun getItemCount(): Int = mItems.size

}