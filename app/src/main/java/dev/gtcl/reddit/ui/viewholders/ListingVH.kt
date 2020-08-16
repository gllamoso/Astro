package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemListingBinding
import dev.gtcl.reddit.models.reddit.listing.Listing
import dev.gtcl.reddit.actions.ListingTypeClickListener

class ListingVH private constructor(private val binding: ItemListingBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(listing: Listing, listingTypeClickListener: ListingTypeClickListener){
        binding.listing = listing
        binding.root.setOnClickListener {
            listingTypeClickListener.listingTypeClicked(listing)
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): ListingVH {
            return ListingVH(ItemListingBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}