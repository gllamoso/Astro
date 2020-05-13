package dev.gtcl.reddit.ui.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.MessageActions
import dev.gtcl.reddit.databinding.ItemMessageBinding
import dev.gtcl.reddit.models.reddit.Message

class MessageViewHolder private constructor(private val binding: ItemMessageBinding): RecyclerView.ViewHolder(binding.root){
    fun bind(message: Message, messageActions: MessageActions){
        binding.message = message
        binding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): MessageViewHolder {
            return MessageViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}