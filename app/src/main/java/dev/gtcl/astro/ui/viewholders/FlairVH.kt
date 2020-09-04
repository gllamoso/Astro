package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.databinding.ItemEditableBinding
import dev.gtcl.astro.models.reddit.listing.Flair
import dev.gtcl.astro.ui.fragments.flair.FlairListAdapter

class FlairVH private constructor(private val binding: ItemEditableBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(flair: Flair, flairSelectionListener: FlairListAdapter.FlairSelectionListener){
        binding.text = flair.text
        binding.isEditable = flair.textEditable

        binding.root.setOnClickListener {
            flairSelectionListener.onSelect(flair)
        }

        binding.itemEditableIcon.setOnClickListener {
            flairSelectionListener.onEdit(flair)
        }

        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): FlairVH{
            return FlairVH(ItemEditableBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }

}