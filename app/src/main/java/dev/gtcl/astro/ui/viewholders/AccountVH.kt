package dev.gtcl.astro.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.SubredditActions
import dev.gtcl.astro.databinding.ItemAccountBinding
import dev.gtcl.astro.models.reddit.listing.Account

class AccountVH private constructor(private val binding: ItemAccountBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        account: Account,
        subredditActions: SubredditActions,
        itemClickListener: ItemClickListener
    ) {
        binding.account = account

        binding.itemAccountAddButton.setOnClickListener {
            subredditActions.subscribe(account.subreddit, account.subreddit.userSubscribed ?: false)
            binding.invalidateAll()
        }

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