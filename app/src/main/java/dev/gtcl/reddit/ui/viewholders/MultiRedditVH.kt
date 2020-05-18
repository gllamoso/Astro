package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.database.DbMultiReddit
import dev.gtcl.reddit.databinding.ItemMultiredditBinding

class MultiRedditVH private constructor(private val binding: ItemMultiredditBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(multiReddit: DbMultiReddit, itemClickListener: ItemClickListener){
        binding.multi = multiReddit
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(multiReddit.asDomainModel())
        }
        binding.editButton.setOnClickListener {

        }
        binding.removeButton.setOnClickListener {

        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent:ViewGroup): MultiRedditVH{
            return MultiRedditVH(ItemMultiredditBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}