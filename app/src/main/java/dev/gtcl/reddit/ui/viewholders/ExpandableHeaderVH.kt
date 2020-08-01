package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemExpandibleBinding
import dev.gtcl.reddit.rotateView

class ExpandableHeaderVH private constructor(private val binding: ItemExpandibleBinding): RecyclerView.ViewHolder(binding.root){
    fun bind(expandableItem: ExpandableItem, showTopDivider: Boolean){
        binding.expandableItem = expandableItem
        binding.showTopDivider = showTopDivider
        binding.executePendingBindings()

        binding.root.setOnClickListener {
            expandableItem.apply {
                if(expandable){
                    expanded = !expanded
                    rotateView(binding.collapseIndicator, expanded)
                    onExpand(expanded)
                }
            }
        }
    }

    companion object{
        fun create(parent: ViewGroup): ExpandableHeaderVH {
            return ExpandableHeaderVH(ItemExpandibleBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}

abstract class ExpandableItem(
    val name: String,
    val expandable: Boolean = true,
    var expanded: Boolean = true
) {
    abstract fun onExpand(expand: Boolean)
}