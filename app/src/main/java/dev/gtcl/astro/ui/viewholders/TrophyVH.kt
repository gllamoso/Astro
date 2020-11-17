package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.databinding.ItemAwardBinding
import dev.gtcl.astro.models.reddit.listing.Trophy

class TrophyVH private constructor(private val binding: ItemAwardBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(trophy: Trophy) {
        binding.award = trophy
        binding.executePendingBindings()
    }

    companion object {
        fun create(parent: ViewGroup) =
            TrophyVH(ItemAwardBinding.inflate(LayoutInflater.from(parent.context)))
    }

}