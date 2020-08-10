package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.MessageActions
import dev.gtcl.reddit.databinding.ItemMessageBinding
import dev.gtcl.reddit.models.reddit.listing.Message
import io.noties.markwon.Markwon

class MessageVH private constructor(private val binding: ItemMessageBinding): RecyclerView.ViewHolder(binding.root){
    fun bind(message: Message, markwon: Markwon?, messageActions: MessageActions, itemClickListener: ItemClickListener){
        binding.message = message
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(message, adapterPosition)
        }
        if(markwon != null){
            markwon.setMarkdown(binding.messageBody, message.body)
        } else {
            binding.messageBody.text = message.bodyFormatted
        }

        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): MessageVH {
            return MessageVH(ItemMessageBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}