package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemSectionHeaderBinding

class SectionHeaderVH private constructor(private val binding: ItemSectionHeaderBinding): RecyclerView.ViewHolder(binding.root){
    fun bind(sectionHeader: SectionHeader, showTopDivider: Boolean = true){
        binding.header = sectionHeader
        binding.showTopDivider = showTopDivider
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

abstract class SectionHeader(
    val name: String,
    collapsed: Boolean = false
) {
    var isCollapsed: Boolean = collapsed
        set(collapse){
            field = collapse
            if(collapse != null) {
                onCollapse(collapse)
            }
        }

    abstract fun onCollapse(collapse: Boolean)
}