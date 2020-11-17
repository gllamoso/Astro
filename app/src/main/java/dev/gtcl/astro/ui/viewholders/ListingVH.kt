package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.databinding.ItemListingBinding
import dev.gtcl.astro.models.reddit.listing.PostListing
import dev.gtcl.astro.actions.ListingTypeClickListener

class ListingVH private constructor(private val binding: ItemListingBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(postListing: PostListing, listingTypeClickListener: ListingTypeClickListener) {
        binding.listing = postListing
        binding.root.setOnClickListener {
            listingTypeClickListener.listingTypeClicked(postListing)
        }
        binding.executePendingBindings()
    }

    companion object {
        fun create(parent: ViewGroup): ListingVH {
            return ListingVH(ItemListingBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}