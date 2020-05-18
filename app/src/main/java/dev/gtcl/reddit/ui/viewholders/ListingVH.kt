package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemListingBinding
import dev.gtcl.reddit.models.reddit.ListingType
import dev.gtcl.reddit.actions.ListingTypeClickListener

class ListingVH private constructor(private val binding: ItemListingBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(listingType: ListingType, listingTypeClickListener: ListingTypeClickListener){
        binding.listing = listingType
        binding.root.setOnClickListener {
            listingTypeClickListener.onClick(listingType)
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): ListingVH {
            return ListingVH(ItemListingBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}