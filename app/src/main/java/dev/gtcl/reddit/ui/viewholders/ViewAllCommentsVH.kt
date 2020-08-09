package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemViewAllCommentsBinding

class ViewAllCommentsVH private constructor(private val binding: ItemViewAllCommentsBinding): RecyclerView.ViewHolder(binding.root){
    fun bind(onClick: () -> Unit){
        binding.background.setOnClickListener {
            onClick()
        }
    }

    companion object{
        fun create(parent: ViewGroup) = ViewAllCommentsVH(ItemViewAllCommentsBinding.inflate(LayoutInflater.from(parent.context)))
    }

}