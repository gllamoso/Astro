package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.databinding.ItemAccountBinding
import dev.gtcl.reddit.models.reddit.Account

class AccountVH private constructor(private val binding: ItemAccountBinding): RecyclerView.ViewHolder(binding.root){

    fun bind(account: Account, itemClickListener: ItemClickListener){
        binding.account = account

        binding.addButton.setOnClickListener {
            account.isSubscribed = !account.isSubscribed
            binding.invalidateAll()
        }

        binding.favoriteButton.setOnClickListener {
            account.isFavorite = !account.isFavorite
            binding.invalidateAll()
        }

        binding.root.setOnClickListener {
            itemClickListener.itemClicked(account)
        }

        binding.executePendingBindings()
    }


    companion object{
        fun create(parent: ViewGroup) = AccountVH(ItemAccountBinding.inflate(LayoutInflater.from(parent.context)))
    }
}