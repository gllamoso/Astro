package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.SubscriptionType
import dev.gtcl.reddit.actions.ListingTypeClickListener
import dev.gtcl.reddit.actions.SubscriptionActions
import dev.gtcl.reddit.actions.SubscriptionAdapterActions
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.databinding.ItemSubscriptionBinding
import dev.gtcl.reddit.models.reddit.listing.SubscriptionListing

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
            sub.isFavorite = !sub.isFavorite
            if(sub.isFavorite){
                subscriptionAdapterActions.addToFavorites(sub)
            } else {
                subscriptionAdapterActions.removeFromFavorites(sub, inFavoritesSection)
            }
            subscriptionActions.favorite(sub, sub.isFavorite)
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