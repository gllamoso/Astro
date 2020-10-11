package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.MultiRedditActions
import dev.gtcl.astro.databinding.ItemMultiredditBinding
import dev.gtcl.astro.models.reddit.listing.MultiReddit
import io.noties.markwon.Markwon

class MultiRedditVH private constructor(private val binding: ItemMultiredditBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        multi: MultiReddit,
        itemClickListener: ItemClickListener,
        multiRedditActions: MultiRedditActions,
        markwon: Markwon
    ) {
        binding.multi = multi
        markwon.setMarkdown(binding.itemMultiRedditDescription, multi.description)
        binding.executePendingBindings()
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(multi, adapterPosition)
        }
        binding.itemMultiRedditInfoButton.setOnClickListener {
            multiRedditActions.viewMoreInfo(multi)
        }
    }

    companion object {
        fun create(parent: ViewGroup) =
            MultiRedditVH(ItemMultiredditBinding.inflate(LayoutInflater.from(parent.context)))
    }
}