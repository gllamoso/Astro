package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemListingBinding
import dev.gtcl.reddit.models.reddit.ListingType
import dev.gtcl.reddit.actions.ListingActions

class ListingViewHolder(private val binding: ItemListingBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(listingType: ListingType, listingActions: ListingActions){
        binding.listing = listingType
        binding.root.setOnClickListener {
            listingActions.onListingClicked(listingType)
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): ListingViewHolder {
            return ListingViewHolder(ItemListingBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}