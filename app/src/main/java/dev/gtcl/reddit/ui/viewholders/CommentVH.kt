package dev.gtcl.reddit.ui.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.reddit.actions.CommentActions
import dev.gtcl.reddit.actions.ItemClickListener
import dev.gtcl.reddit.databinding.ItemCommentBinding
import dev.gtcl.reddit.databinding.LayoutPopupCommentOptionsBinding
import dev.gtcl.reddit.models.reddit.listing.Comment
import io.noties.markwon.Markwon

class CommentVH private constructor(private val binding: ItemCommentBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(comment: Comment, markwon: Markwon, commentActions: CommentActions, itemClickListener: ItemClickListener){
        binding.comment = comment
        itemView.setOnClickListener {
            itemClickListener.itemClicked(comment, adapterPosition)
        }
        if(comment.hiddenPoints > 0){
            itemView.visibility = View.GONE
            itemView.layoutParams = RecyclerView.LayoutParams(0,0)
        }
        else {
            itemView.visibility = View.VISIBLE
            itemView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        binding.moreOptions.setOnClickListener {
            showPopupWindow(comment, commentActions, it)
        }
        markwon.setMarkdown(binding.bodyMessage, comment.body)
        binding.bodyMessage.setOnClickListener {
            itemClickListener.itemClicked(comment, adapterPosition)
        }
        binding.executePendingBindings()
    }

    private fun showPopupWindow(comment: Comment, commentActions: CommentActions, anchorView: View){
        val inflater = anchorView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = LayoutPopupCommentOptionsBinding.inflate(inflater)
        val popupWindow = PopupWindow(popupBinding.root, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
        popupBinding.apply {
            this.comment = comment
            upvoteButton.root.setOnClickListener {
                popupWindow.dismiss()
            }
            downvoteButton.root.setOnClickListener {
                popupWindow.dismiss()
            }
            replyButton.root.setOnClickListener {
                commentActions.reply(comment, adapterPosition)
                popupWindow.dismiss()
            }
            saveButton.root.setOnClickListener {
                popupWindow.dismiss()
            }
            profileButton.root.setOnClickListener {
                commentActions.viewProfile(comment)
                popupWindow.dismiss()
            }
            shareButton.root.setOnClickListener {
                popupWindow.dismiss()
            }
            reportButton.root.setOnClickListener {
                popupWindow.dismiss()
            }
        }

        popupBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.height = popupBinding.root.measuredHeight
        popupWindow.elevation = 20F
        popupWindow.showAsDropDown(anchorView)
        popupBinding.executePendingBindings()
    }

    companion object{
        fun create(parent: ViewGroup): CommentVH {
            return CommentVH(ItemCommentBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}