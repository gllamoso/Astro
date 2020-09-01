package dev.gtcl.astro.ui.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.actions.MessageActions
import dev.gtcl.astro.databinding.ItemMessageBinding
import dev.gtcl.astro.databinding.PopupMessageActionsBinding
import dev.gtcl.astro.models.reddit.listing.Message
import dev.gtcl.astro.showAsDropdown
import io.noties.markwon.Markwon

class MessageVH private constructor(private val binding: ItemMessageBinding): RecyclerView.ViewHolder(binding.root){
    fun bind(message: Message, markwon: Markwon?, messageActions: MessageActions, username: String?, itemClickListener: ItemClickListener){
        binding.message = message
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(message, adapterPosition)
        }
        if(markwon != null){
            markwon.setMarkdown(binding.itemMessageBody, message.body)
        } else {
            binding.itemMessageBody.text = message.bodyFormatted
        }

        binding.itemMessageMoreOptions.setOnClickListener {
            showPopupWindow(message, messageActions, username == message.author, it)
        }

        binding.executePendingBindings()
    }

    private fun showPopupWindow(message: Message, messageActions: MessageActions, createdFromUser: Boolean, anchor: View){
        val inflater = anchor.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupMessageActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.message = message
            this.createdFromUser = createdFromUser
            popupMessageActionsMark.root.setOnClickListener {
                messageActions.mark(message, message.new)
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupMessageActionsReply.root.setOnClickListener {
                messageActions.reply(message)
                popupWindow.dismiss()
            }
            popupMessageActionsProfile.root.setOnClickListener {
                messageActions.viewProfile(message)
                popupWindow.dismiss()
            }
            popupMessageActionsDelete.root.setOnClickListener {
                messageActions.delete(message, adapterPosition)
                popupWindow.dismiss()
            }

            if(!createdFromUser){
                popupMessageActionsBlock.root.setOnClickListener {
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