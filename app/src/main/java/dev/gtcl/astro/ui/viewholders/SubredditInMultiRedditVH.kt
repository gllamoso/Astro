package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.ItemSubredditInMultiredditBinding
import dev.gtcl.astro.models.reddit.listing.Subreddit

class SubredditInMultiRedditVH private constructor(private val binding: ItemSubredditInMultiredditBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        sub: Subreddit,
        itemClickListener: ItemClickListener,
        onSubredditRemovedListener: OnSubredditRemovedListener?
    ) {
        binding.sub = sub
        binding.isRemovable = onSubredditRemovedListener != null
        onSubredditRemovedListener?.let { listener ->
            binding.itemSubredditInMultiRedditRemoveButton.setOnClickListener {
                listener.onRemove(sub, adapterPosition)
            }
        }
        binding.root.setOnClickListener {
            itemClickListener.clicked(sub, adapterPosition)
        }
        binding.executePendingBindings()
    }

    companion object {
        fun create(parent: ViewGroup) =
            SubredditInMultiRedditVH(
                ItemSubredditInMultiredditBinding.inflate(LayoutInflater.from(parent.context))
            )
    }
}

interface OnSubredditRemovedListener {
    fun onRemove(subreddit: Subreddit, position: Int)
}