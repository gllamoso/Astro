package dev.gtcl.reddit.ui.viewholders

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemLeftDrawerBinding

class LeftDrawerItemVH private constructor(private val binding: ItemLeftDrawerBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(selected: Boolean, name: String, leftIcon: Drawable?, rightIcon: Drawable?, onClick: () -> (Unit), onRightIconClicked: (() -> (Unit))? = null){
        binding.name = name
        binding.leftIcon = leftIcon
        binding.rightIcon = rightIcon
        binding.isSelected = selected

        binding.root.setOnClickListener {
            onClick()
        }

        if(onRightIconClicked != null){
            binding.itemLeftDrawerRightIcon.setOnClickListener {
                onRightIconClicked()
            }
        }

        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup) = LeftDrawerItemVH(ItemLeftDrawerBinding.inflate(LayoutInflater.from(parent.context)))
    }
}