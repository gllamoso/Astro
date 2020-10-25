package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.ItemAccountBinding
import dev.gtcl.astro.models.reddit.listing.Account

class AccountVH private constructor(private val binding: ItemAccountBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        account: Account,
        itemClickListener: ItemClickListener
    ) {
        binding.account = account

        binding.root.setOnClickListener {
            itemClickListener.itemClicked(account, adapterPosition)
        }

        binding.executePendingBindings()
    }


    companion object {
        fun create(parent: ViewGroup) =
            AccountVH(ItemAccountBinding.inflate(LayoutInflater.from(parent.context)))
    }
}