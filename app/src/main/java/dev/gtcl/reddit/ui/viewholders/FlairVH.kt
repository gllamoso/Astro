package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.databinding.ItemEditableBinding
import dev.gtcl.reddit.models.reddit.listing.Flair
import dev.gtcl.reddit.ui.fragments.flair.FlairListAdapter

class FlairVH private constructor(private val binding: ItemEditableBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(flair: Flair, flairSelectionListener: FlairListAdapter.FlairSelectionListener){
        binding.text = flair.text
        binding.isEditable = flair.textEditable

        binding.root.setOnClickListener {
            flairSelectionListener.onSelect(flair)
        }

        binding.itemEditableText.setOnClickListener {
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