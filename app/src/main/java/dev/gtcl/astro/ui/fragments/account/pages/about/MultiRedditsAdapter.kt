package dev.gtcl.astro.ui.fragments.account.pages.about

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.MultiRedditActions
import dev.gtcl.astro.models.reddit.listing.MultiReddit
import dev.gtcl.astro.ui.viewholders.MultiRedditVH
import io.noties.markwon.Markwon

class MultiRedditsAdapter(
    private val itemClickListener: ItemClickListener,
    private val multiRedditActions: MultiRedditActions,
    private val markwon: Markwon
) : ListAdapter<MultiReddit, MultiRedditVH>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiRedditVH =
        MultiRedditVH.create(parent)

    override fun onBindViewHolder(holder: MultiRedditVH, position: Int) {
        holder.bind(getItem(position), itemClickListener, multiRedditActions, markwon)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MultiReddit>() {
            override fun areItemsTheSame(oldItem: MultiReddit, newItem: MultiReddit) =
                oldItem.pathFormatted == newItem.pathFormatted

            override fun areContentsTheSame(oldItem: MultiReddit, newItem: MultiReddit) =
                oldItem == newItem
        }
    }
}