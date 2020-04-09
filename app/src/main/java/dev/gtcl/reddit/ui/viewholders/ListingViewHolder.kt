package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemListingBinding
import dev.gtcl.reddit.listings.ListingType
import dev.gtcl.reddit.ui.fragments.home.listing.subreddits.ListingOnClickListeners

class ListingViewHolder(private val binding: ItemListingBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(listingType: ListingType, listingOnClickListeners: ListingOnClickListeners){
        binding.listing = listingType
        binding.root.setOnClickListener {
            listingOnClickListeners.onClick(listingType)
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): ListingViewHolder {
            return ListingViewHolder(ItemListingBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}