package dev.gtcl.astro.ui.fragments.account.pages.about

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.SubredditActions
import dev.gtcl.astro.models.reddit.listing.SubredditInModeratedList
import dev.gtcl.astro.ui.viewholders.SubredditVH

class ModeratedSubsAdapter(
    private val subredditActions: SubredditActions,
    private val itemClickListener: ItemClickListener
) :
    ListAdapter<SubredditInModeratedList, SubredditVH>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SubredditVH.create(parent)

    override fun onBindViewHolder(holder: SubredditVH, position: Int) {
        holder.bind(getItem(position), subredditActions, itemClickListener)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SubredditInModeratedList>() {
            override fun areItemsTheSame(
                oldItem: SubredditInModeratedList,
                newItem: SubredditInModeratedList
            ) = oldItem.name == newItem.name

            override fun areContentsTheSame(
                oldItem: SubredditInModeratedList,
                newItem: SubredditInModeratedList
            ) = oldItem == newItem
        }
    }
}