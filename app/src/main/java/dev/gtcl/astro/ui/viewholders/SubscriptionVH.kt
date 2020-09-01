package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.SubscriptionType
import dev.gtcl.astro.actions.ListingTypeClickListener
import dev.gtcl.astro.actions.SubscriptionActions
import dev.gtcl.astro.actions.SubscriptionAdapterActions
import dev.gtcl.astro.database.Subscription
import dev.gtcl.astro.databinding.ItemSubscriptionBinding
import dev.gtcl.astro.models.reddit.listing.SubscriptionListing

class SubscriptionVH private constructor(private val binding: ItemSubscriptionBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(
        sub: Subscription,
        listingTypeClickListener: ListingTypeClickListener,
        subscriptionAdapterActions: SubscriptionAdapterActions,
        subscriptionActions: SubscriptionActions,
        inFavoritesSection: Boolean = false){

        binding.sub = sub
        binding.root.setOnClickListener {
            listingTypeClickListener.listingTypeClicked(
                SubscriptionListing(sub)
            )
        }

        if(sub.type == SubscriptionType.MULTIREDDIT){
            binding.itemSubscriptionEditButton.setOnClickListener {
               subscriptionActions.editMultiReddit(sub)
            }
        }

        binding.itemSubscriptionFavoriteButton.setOnClickListener {
            subscriptionActions.favorite(sub, !sub.isFavorite, inFavoritesSection)
            binding.invalidateAll()
        }

        binding.itemSubscriptionRemoveButton.setOnClickListener {
            subscriptionAdapterActions.remove(sub)
            subscriptionActions.remove(sub)
        }
    }

    companion object{
        fun create(parent: ViewGroup) = SubscriptionVH(ItemSubscriptionBinding.inflate(LayoutInflater.from(parent.context)))
    }
}