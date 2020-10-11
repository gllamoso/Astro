package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.ItemSubredditBinding
import dev.gtcl.astro.actions.SubredditActions
import dev.gtcl.astro.models.reddit.listing.Subreddit
import dev.gtcl.astro.models.reddit.listing.SubredditInModeratedList

class SubredditVH private constructor(private val binding: ItemSubredditBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(
        sub: Subreddit,
        subredditActions: SubredditActions?,
        itemClickListener: ItemClickListener
    ) {
        binding.apply {
            displayName = sub.displayName
            icon = sub.icon
            title = sub.title
            subscribers = sub.subscribers
            moreInfoAvailable = subredditActions != null
        }
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(sub, adapterPosition)
        }
        subredditActions?.let {
            binding.itemSubredditInfoButton.setOnClickListener {
                subredditActions.viewMoreInfo(sub.displayName)
            }
        }
        binding.executePendingBindings()
    }

    fun bind(
        sub: SubredditInModeratedList,
        subredditActions: SubredditActions,
        itemClickListener: ItemClickListener
    ) {
        binding.apply {
            displayName = sub.displayName
            icon = sub.icon
            title = sub.title
            subscribers = sub.subscribers
        }
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(sub, adapterPosition)
        }
        binding.itemSubredditInfoButton.setOnClickListener {
            subredditActions.viewMoreInfo(sub.displayName)
        }
        binding.executePendingBindings()
    }

    companion object {
        fun create(parent: ViewGroup) =
            SubredditVH(ItemSubredditBinding.inflate(LayoutInflater.from(parent.context)))
    }
}