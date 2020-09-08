package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.databinding.ItemListingBinding
import dev.gtcl.astro.models.reddit.listing.Listing
import dev.gtcl.astro.actions.ListingTypeClickListener

class ListingVH private constructor(private val binding: ItemListingBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(listing: Listing, listingTypeClickListener: ListingTypeClickListener) {
        binding.listing = listing
        binding.root.setOnClickListener {
            listingTypeClickListener.listingTypeClicked(listing)
        }
        binding.executePendingBindings()
    }

    companion object {
        fun create(parent: ViewGroup): ListingVH {
            return ListingVH(ItemListingBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}