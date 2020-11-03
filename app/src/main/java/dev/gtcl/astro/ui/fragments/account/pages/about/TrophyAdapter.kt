package dev.gtcl.astro.ui.fragments.account.pages.about

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import dev.gtcl.astro.models.reddit.listing.Trophy
import dev.gtcl.astro.ui.viewholders.TrophyVH

class TrophyAdapter : ListAdapter<Trophy, TrophyVH>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrophyVH =
        TrophyVH.create(parent)

    override fun onBindViewHolder(holder: TrophyVH, position: Int) {
        holder.bind(getItem(position))
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Trophy>() {
            override fun areItemsTheSame(oldItem: Trophy, newItem: Trophy) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Trophy, newItem: Trophy) = oldItem == newItem
        }
    }
}