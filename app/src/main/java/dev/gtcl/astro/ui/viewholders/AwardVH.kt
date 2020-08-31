package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.databinding.ItemAwardBinding
import dev.gtcl.astro.models.reddit.listing.Award

class AwardVH private constructor(private val binding: ItemAwardBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(award: Award){
        binding.award = award
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup) = AwardVH(ItemAwardBinding.inflate(LayoutInflater.from(parent.context)))
    }

}