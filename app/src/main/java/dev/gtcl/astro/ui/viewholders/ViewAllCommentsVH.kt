package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.databinding.ItemViewAllCommentsBinding

class ViewAllCommentsVH private constructor(private val binding: ItemViewAllCommentsBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(onClick: () -> Unit) {
        binding.itemViewAllCommentsBackground.setOnClickListener {
            onClick()
        }
    }

    companion object {
        fun create(parent: ViewGroup) =
            ViewAllCommentsVH(ItemViewAllCommentsBinding.inflate(LayoutInflater.from(parent.context)))
    }

}