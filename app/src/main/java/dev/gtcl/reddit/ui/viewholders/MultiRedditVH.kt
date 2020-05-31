package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.database.Subscription
import dev.gtcl.reddit.databinding.ItemSubscriptionBinding

class MultiRedditVH private constructor(private val binding: ItemSubscriptionBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(itemClickListener: ItemClickListener){
//        binding.multi = multiReddit
        binding.root.setOnClickListener {
//            itemClickListener.itemClicked(multiReddit.asDomainModel())
        }
        binding.editButton.setOnClickListener {

        }
        binding.removeButton.setOnClickListener {

        }
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent:ViewGroup): MultiRedditVH{
            return MultiRedditVH(ItemSubscriptionBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}