package dev.gtcl.astro.ui.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import dev.gtcl.astro.Vote
import dev.gtcl.astro.actions.CommentActions
import dev.gtcl.astro.actions.ItemClickListener
import dev.gtcl.astro.databinding.ItemCommentBinding
import dev.gtcl.astro.databinding.PopupCommentActionsBinding
import dev.gtcl.astro.models.reddit.listing.Comment
import dev.gtcl.astro.showAsDropdown
import io.noties.markwon.Markwon

class CommentVH private constructor(private val binding: ItemCommentBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(comment: Comment, markwon: Markwon, commentActions: CommentActions, userId: String?, itemClickListener: ItemClickListener){
        binding.comment = comment
        binding.itemCommentBackground.setOnClickListener {
            itemClickListener.itemClicked(comment, adapterPosition)
        }
        binding.itemCommentMoreOptions.setOnClickListener {
            showPopupWindow(comment, commentActions, (userId != null && comment.authorFullName == userId), it)
        }
        markwon.setMarkdown(binding.itemCommentBodyMessage, comment.body)
        binding.executePendingBindings()
    }

    private fun showPopupWindow(comment: Comment, commentActions: CommentActions, createdFromUser: Boolean, anchor: View){
        val inflater = anchor.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupBinding = PopupCommentActionsBinding.inflate(inflater)
        val popupWindow = PopupWindow()
        popupBinding.apply {
            this.comment = comment
            this.createdFromUser = createdFromUser
            if(createdFromUser){
                popupCommentActionsEdit.root.setOnClickListener {
                    commentActions.edit(comment, adapterPosition)
                    popupWindow.dismiss()
                }
                popupCommentActionsDelete.root.setOnClickListener {
                    commentActions.delete(comment, adapterPosition)
                    popupWindow.dismiss()
                }
            }
            popupCommentActionsUpvote.root.setOnClickListener {
                commentActions.vote(comment, if(comment.likes == true) Vote.UNVOTE else Vote.UPVOTE)
                comment.likes = if(comment.likes == true) {
                    null
                } else {
                    true
                }
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupCommentActionsDownvote.root.setOnClickListener {
                commentActions.vote(comment, if(comment.likes == false) Vote.UNVOTE else Vote.DOWNVOTE)
                comment.likes = if(comment.likes == false) {
                    null
                } else {
                    false
                }
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupCommentActionsReply.root.setOnClickListener {
                commentActions.reply(comment, adapterPosition)
                popupWindow.dismiss()
            }
            popupCommentActionsSave.root.setOnClickListener {
                comment.saved = comment.saved != true
                commentActions.save(comment)
                binding.invalidateAll()
                popupWindow.dismiss()
            }
            popupCommentActionsProfile.root.setOnClickListener {
                commentActions.viewProfile(comment)
                popupWindow.dismiss()
            }
            popupCommentActionsShare.root.setOnClickListener {
                commentActions.share(comment)
                popupWindow.dismiss()
            }
            popupCommentActionsReport.root.setOnClickListener {
                commentActions.report(comment, adapterPosition)
                popupWindow.dismiss()
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
        fun create(parent: ViewGroup): CommentVH {
            return CommentVH(ItemCommentBinding.inflate(LayoutInflater.from(parent.context)))
        }
    }
}