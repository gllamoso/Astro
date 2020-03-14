package dev.gtcl.reddit.ui.fragments.subreddits.mine

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.R
import dev.gtcl.reddit.databinding.ItemListingBinding
import dev.gtcl.reddit.databinding.ItemSectionHeaderBinding
import dev.gtcl.reddit.listings.ListingType
import dev.gtcl.reddit.listings.SubredditListing
import dev.gtcl.reddit.subs.Subreddit
import dev.gtcl.reddit.ui.fragments.subreddits.SubredditOnClickListener

class MultiAndSubsListAdapter(private val context: Context, private val subredditOnClickListener: SubredditOnClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private var mItems: MutableList<Any> = mutableListOf()

    fun submitLists(multiReddits: List<ListingType>, subs: List<Subreddit>){
        mItems = mutableListOf()
        mItems.add(context.resources.getText(R.string.multireddits).toString())
        mItems.addAll(multiReddits)
        mItems.add(context.resources.getText(R.string.subreddits).toString())
        mItems.addAll(subs)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType){
            R.layout.item_section_header -> SectionTitleViewHolder.create(parent)
            R.layout.item_listing -> ListingViewHolder.create(parent)
            else -> throw IllegalArgumentException("unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(val item = mItems[position]) {
            is String -> (holder as SectionTitleViewHolder).bind(item)
            is ListingType -> (holder as ListingViewHolder).bind(item, subredditOnClickListener)
            is Subreddit -> (holder as ListingViewHolder).bind(SubredditListing(item), subredditOnClickListener)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(mItems[position]){
            is String -> R.layout.item_section_header
            is ListingType -> R.layout.item_listing
            is Subreddit -> R.layout.item_listing
            else -> throw IllegalArgumentException("Invalid item: ${mItems[position].javaClass.simpleName}")
        }
    }

    override fun getItemCount(): Int = mItems.size

    class ListingViewHolder(private val binding: ItemListingBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(listingType: ListingType, subredditOnClickListener: SubredditOnClickListener){
            binding.listing = listingType
            binding.root.setOnClickListener {
                subredditOnClickListener.onClick(listingType)
            }
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup): ListingViewHolder {
                return ListingViewHolder(ItemListingBinding.inflate(LayoutInflater.from(parent.context)))
            }
        }
    }

    class SectionTitleViewHolder(private val binding: ItemSectionHeaderBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(header: String){
            binding.header = header
            binding.executePendingBindings()
        }

        companion object{
            fun create(parent: ViewGroup): SectionTitleViewHolder {
                return SectionTitleViewHolder(ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context)))
            }
        }
    }



}