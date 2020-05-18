package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemSectionHeaderBinding
import dev.gtcl.reddit.ui.fragments.subreddits.mine.SectionHeader

class SectionHeaderVH private constructor(private val binding: ItemSectionHeaderBinding): RecyclerView.ViewHolder(binding.root){
    fun bind(sectionHeader: SectionHeader){
        binding.header = sectionHeader
        binding.executePendingBindings()

        sectionHeader.isCollapsed?.let {
            binding.root.setOnClickListener {
                sectionHeader.apply {
                    isCollapsed = !isCollapsed!!
                    rotateCollapseIndicator(isCollapsed!!)
                }
            }
        }
    }

    private fun rotateCollapseIndicator(collapse: Boolean){
        binding.collapseIndicator.animate().rotation(if(collapse) {
            180F
        } else {
            0F
        })
    }

    companion object{
        fun create(parent: ViewGroup): SectionHeaderVH {
            return SectionHeaderVH(ItemSectionHeaderBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}