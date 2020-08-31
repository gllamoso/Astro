package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.ItemSubredditBinding
import dev.gtcl.astro.actions.SubredditActions
import dev.gtcl.astro.models.reddit.listing.Subreddit

class SubredditVH private constructor(private val binding: ItemSubredditBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(sub: Subreddit, subredditActions: SubredditActions, itemClickListener: ItemClickListener){
        binding.sub = sub
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(sub, adapterPosition)
        }
        binding.itemSubredditAddButton.setOnClickListener{
            sub.userSubscribed = sub.userSubscribed != true
            subredditActions.subscribe(sub, sub.userSubscribed!!)
            binding.invalidateAll()
        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup) = SubredditVH(ItemSubredditBinding.inflate(LayoutInflater.from(parent.context)))
    }
}