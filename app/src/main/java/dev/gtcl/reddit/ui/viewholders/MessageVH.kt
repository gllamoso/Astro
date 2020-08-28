package dev.gtcl.reddit.ui.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.Vote
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.actions.MessageActions
import dev.gtcl.reddit.actions.PostActions
import dev.gtcl.reddit.databinding.ItemMessageBinding
import dev.gtcl.reddit.databinding.PopupMessageOptionsBinding
import dev.gtcl.reddit.databinding.PopupPostOptionsBinding
import dev.gtcl.reddit.models.reddit.listing.Message
import dev.gtcl.reddit.models.reddit.listing.Post
import dev.gtcl.reddit.showAsDropdown
import io.noties.markwon.Markwon

class MessageVH private constructor(private val binding: ItemMessageBinding): RecyclerView.ViewHolder(binding.root){
    fun bind(message: Message, markwon: Markwon?, messageActions: MessageActions, username: String?, itemClickListener: ItemClickListener){
        binding.message = message
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(message, adapterPosition)
        }
        if(markwon != null){
            markwon.setMarkdown(binding.messageBody, message.body)
        } else {
            binding.messageBody.text = message.bodyFormatted
        }

        binding.moreOptions.setOnClickListener {
            showPopupWindow(message, messageActions, username == message.author, it)
        }

        binding.executePendingBindings()
    }

    private fun showPopupWindow(message: Message, messageActions: MessageActions, createdFromUser: Boolean, anchor: View){
        val inflater = anchor.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupMessageOptionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.message = message
            this.createdFromUser = createdFromUser
            markButton.root.setOnClickListener {
                messageActions.mark(message, message.new)
                message.new = !message.new
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            replyButton.root.setOnClickListener {
                messageActions.reply(message)
                popupWindow.dismiss()
            }
            profileButton.root.setOnClickListener {
                messageActions.viewProfile(message)
                popupWindow.dismiss()
            }
            deleteButton.root.setOnClickListener {
                messageActions.delete(message, adapterPosition)
                popupWindow.dismiss()
            }

            if(!createdFromUser){
                blockButton.root.setOnClickListener {
                    messageActions.block(message, adapterPosition)
                    popupWindow.dismiss()
                }
            }
            executePendingBindings()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }

        popupWindow.showAsDropdown(anchor, popupBinding.root, ViewGroup.LayoutParams.WRAP_CONTENT, popupBinding.root.measuredHeight)
    }

    companion object{
        fun create(parent: ViewGroup): MessageVH {
            return MessageVH(ItemMessageBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}