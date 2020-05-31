package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.databinding.ItemSubredditBinding
import dev.gtcl.reddit.models.reddit.Subreddit
import dev.gtcl.reddit.actions.SubredditActions
import dev.gtcl.reddit.actions.SubscriptionAdapterActions

class SubredditVH private constructor(private val binding: ItemSubredditBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(sub: Subreddit, subredditActions: SubredditActions, subscriptionAdapterActions: SubscriptionAdapterActions?, inFavoritesSection: Boolean = false, itemClickListener: ItemClickListener){
        binding.sub = sub
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(sub)
        }
        binding.addButton.setOnClickListener{
            sub.userSubscribed = sub.userSubscribed != true
            if(!sub.userSubscribed!!) {
//                mySubscriptionActionsInAdapter?.remove(sub)
                sub.isFavorite = false
            }
            subredditActions.subscribe(sub, sub.userSubscribed!!)
            binding.invalidateAll()
        }
        binding.favoriteButton.setOnClickListener {
            subredditActions.favorite(sub, !sub.isFavorite)
            sub.isFavorite = !sub.isFavorite
            if(sub.isFavorite) {
                sub.userSubscribed = true
//                mySubscriptionActionsInAdapter?.addToFavorites(sub)
            } else {
//                mySubscriptionActionsInAdapter?.removeFromFavorites(sub, inFavoritesSection)
            }
            binding.invalidateAll()
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): SubredditVH {
            return SubredditVH(ItemSubredditBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}