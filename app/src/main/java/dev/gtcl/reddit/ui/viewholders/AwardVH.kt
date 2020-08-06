package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemAwardBinding
import dev.gtcl.reddit.models.reddit.listing.Award

class AwardVH private constructor(private val binding: ItemAwardBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(award: Award){
        binding.award = award
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup) = AwardVH(ItemAwardBinding.inflate(LayoutInflater.from(parent.context)))
    }

}