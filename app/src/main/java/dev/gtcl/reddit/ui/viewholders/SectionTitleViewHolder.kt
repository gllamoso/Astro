package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemSectionHeaderBinding

class SectionTitleViewHolder(private val binding: ItemSectionHeaderBinding): RecyclerView.ViewHolder(binding.root){
    fun bind(header: String){
        binding.header = header
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): SectionTitleViewHolder {
            return SectionTitleViewHolder(ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}