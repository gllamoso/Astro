package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.R
import dev.gtcl.astro.databinding.ItemNoItemsFoundBinding
import dev.gtcl.astro.models.reddit.listing.ItemType

class NoItemFoundVH private constructor(private val binding: ItemNoItemsFoundBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(expectedItem: ItemType?){
        binding.text = binding.root.context.applicationContext.getString(
            when(expectedItem){
                ItemType.Comment -> R.string.no_comments_found
                ItemType.Post -> R.string.no_posts_found
                ItemType.Message -> R.string.no_messages_found
                else -> R.string.nothing_found
            }
        )
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): NoItemFoundVH = NoItemFoundVH(ItemNoItemsFoundBinding.inflate(LayoutInflater.from(parent.context)))
    }
}