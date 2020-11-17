package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.MultiRedditActions
import dev.gtcl.astro.databinding.ItemMultiredditBinding
import dev.gtcl.astro.html.createHtmlViews
import dev.gtcl.astro.models.reddit.listing.MultiReddit
import me.saket.bettermovementmethod.BetterLinkMovementMethod

class MultiRedditVH private constructor(private val binding: ItemMultiredditBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        multi: MultiReddit,
        itemClickListener: ItemClickListener,
        multiRedditActions: MultiRedditActions,
        movementMethod: BetterLinkMovementMethod
    ) {
        binding.multi = multi
        binding.itemMultiRedditDescriptionLayout.createHtmlViews(
            multi.parseDescription(),
            null,
            movementMethod
        )
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