package dev.gtcl.astro.ui.fragments.account.pages.about

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import dev.gtcl.astro.models.reddit.listing.Award
import dev.gtcl.astro.ui.viewholders.AwardVH

class AwardsAdapter : ListAdapter<Award, AwardVH>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AwardVH =
        AwardVH.create(parent)

    override fun onBindViewHolder(holder: AwardVH, position: Int) {
        holder.bind(getItem(position))
    }


    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Award>() {
            override fun areItemsTheSame(oldItem: Award, newItem: Award) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: Award, newItem: Award) = oldItem == newItem
        }
    }
}